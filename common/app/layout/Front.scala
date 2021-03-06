package layout

import com.gu.facia.api.models.{CollectionConfig, CuratedContent, FaciaContent}
import conf.Switches
import dfp.{DfpAgent, SponsorshipTag}
import implicits.FaciaContentFrontendHelpers._
import implicits.FaciaContentImplicits._
import model.PressedPage
import model.facia.PressedCollection
import model.meta.{ListItem, ItemList}
import org.joda.time.DateTime
import services.{CollectionConfigWithId, FaciaContentConvert}
import slices.{MostPopular, _}
import views.support.CutOut

import scala.Function._

/** For de-duplicating cutouts */
object ContainerLayoutContext {
  val empty = ContainerLayoutContext(Set.empty, hideCutOuts = false)
}

case class ContainerLayoutContext(
  cutOutsSeen: Set[CutOut],
  hideCutOuts: Boolean
) {
  def addCutOuts(cutOut: Set[CutOut]) = copy(cutOutsSeen = cutOutsSeen ++ cutOut)

  type CardAndContext = (ContentCard, ContainerLayoutContext)

  private def dedupCutOut(cardAndContext: CardAndContext): CardAndContext = {
    val (content, context) = cardAndContext

    if (content.snapStuff.map(_.snapType) == Some(FrontendLatestSnap)) {
      (content, context)
    } else {
      val newCard = if (content.cutOut.exists(cutOutsSeen.contains)) {
        content.copy(cutOut = None)
      } else {
        content
      }
      (newCard, context.addCutOuts(content.cutOut.filter(const(content.cardTypes.showCutOut)).toSet))
    }
  }

  private val transforms = Seq(
    dedupCutOut _
  ).reduce(_ compose _)

  def transform(card: FaciaCardAndIndex) = {
    if (hideCutOuts) {
      (card.transformCard(_.copy(cutOut = None)), this)
    } else {
      // Latest snaps are sometimes used to promote journalists, and the cut out should always show on these snaps.
      card.item match {
        case content: ContentCard =>
          val (newCard, newContext) = transforms((content, this))
          (card.copy(item = newCard), newContext)

        case _ => (card, this)
      }
    }
  }
}

object CollectionEssentials {
  /* FAPI Integration */

  def fromCollection(collection: model.Collection) = CollectionEssentials(
    collection.items.map(FaciaContentConvert.frontentContentToFaciaContent),
    collection.treats.map(FaciaContentConvert.frontentContentToFaciaContent),
    collection.displayName,
    collection.href,
    collection.lastUpdated,
    if (collection.curated.isEmpty) Some(9) else None
  )

  def fromPressedCollection(collection: PressedCollection) = CollectionEssentials(
    collection.all,
    collection.treats,
    Option(collection.displayName),
    collection.href,
    collection.lastUpdated.map(_.toString),
    if (collection.curated.isEmpty) Some(9) else None
  )

  def fromFaciaContent(trails: Seq[FaciaContent]) = CollectionEssentials(
    trails,
    Nil,
    None,
    None,
    None,
    None
  )

  val empty = CollectionEssentials(Nil, Nil, None, None, None, None)
}

case class CollectionEssentials(
  items: Seq[FaciaContent],
  treats: Seq[FaciaContent],
  displayName: Option[String],
  href: Option[String],
  lastUpdated: Option[String],
  showMoreLimit: Option[Int]
)

object ContainerCommercialOptions {
  def fromConfig(config: CollectionConfig) = ContainerCommercialOptions(
    DfpAgent.isSponsored(config),
    DfpAgent.isAdvertisementFeature(config),
    DfpAgent.isFoundationSupported(config),
    DfpAgent.sponsorshipTag(config),
    DfpAgent.sponsorshipType(config)
  )

  val empty = ContainerCommercialOptions(
    isSponsored = false,
    isAdvertisementFeature = false,
    isFoundationSupported = false,
    sponsorshipTag = None,
    sponsorshipType = None
  )
}

case class ContainerCommercialOptions(
  isSponsored: Boolean,
  isAdvertisementFeature: Boolean,
  isFoundationSupported: Boolean,
  sponsorshipTag: Option[SponsorshipTag],
  sponsorshipType: Option[String]
) {
  val isPaidFor = isSponsored || isAdvertisementFeature || isFoundationSupported
}

object FaciaContainer {
  def apply(
    index: Int,
    container: Container,
    config: CollectionConfigWithId,
    collectionEssentials: CollectionEssentials,
    componentId: Option[String] = None
  ): FaciaContainer = {
    apply(
      index,
      container,
      ContainerDisplayConfig.withDefaults(config),
      collectionEssentials,
      componentId
    )
  }

  def apply(
    index: Int,
    container: Container,
    config: ContainerDisplayConfig,
    collectionEssentials: CollectionEssentials,
    componentId: Option[String]
  ): FaciaContainer = fromConfig(
    index,
    container,
    config.collectionConfigWithId,
    collectionEssentials,
    ContainerLayout.fromContainer(
      container,
      ContainerLayoutContext.empty,
      config,
      collectionEssentials.items
    ).map(_._1),
    componentId
  )

  def fromConfig(
    index: Int,
    container: Container,
    config: CollectionConfigWithId,
    collectionEssentials: CollectionEssentials,
    containerLayout: Option[ContainerLayout],
    componentId: Option[String]
  ): FaciaContainer = FaciaContainer(
    index,
    config.id,
    config.config.displayName orElse collectionEssentials.displayName,
    config.config.href orElse collectionEssentials.href,
    componentId,
    container,
    collectionEssentials,
    containerLayout,
    config.config.showDateHeader,
    config.config.showLatestUpdate,
    // popular containers should never be sponsored
    container match {
      case MostPopular => ContainerCommercialOptions.empty
      case _ => ContainerCommercialOptions.fromConfig(config.config)
    },
    None,
    None,
    hideToggle = false,
    showTimestamps = false,
    None,
    useShowMore = true
  )

  def forStoryPackage(dataId: String, items: Seq[FaciaContent], title: String, href: Option[String] = None) = {
    FaciaContainer(
      index = 2,
      container = Fixed(ContainerDefinition.fastForNumberOfItems(items.size)),
      config = ContainerDisplayConfig.withDefaults(CollectionConfigWithId(dataId, CollectionConfig.empty)),
      collectionEssentials = CollectionEssentials(items take 8, Nil, Some(title), href, None, None),
      componentId = None
    ).withTimeStamps
  }
}

case class FaciaContainer(
  index: Int,
  dataId: String,
  displayName: Option[String],
  href: Option[String],
  componentId: Option[String],
  container: Container,
  collectionEssentials: CollectionEssentials,
  containerLayout: Option[ContainerLayout],
  showDateHeader: Boolean,
  showLatestUpdate: Boolean,
  commercialOptions: ContainerCommercialOptions,
  customHeader: Option[FaciaContainerHeader],
  customClasses: Option[Seq[String]],
  hideToggle: Boolean,
  showTimestamps: Boolean,
  dateLinkPath: Option[String],
  useShowMore: Boolean
) {
  def transformCards(f: ContentCard => ContentCard) = copy(
    containerLayout = containerLayout.map(_.transformCards(f))
  )

  def faciaComponentName = componentId getOrElse {
    displayName map { title: String =>
      title.toLowerCase.replace(" ", "-")
    } getOrElse "no-name"
  }

  def latestUpdate = (collectionEssentials.items.flatMap(_.webPublicationDateOption) ++
    collectionEssentials.lastUpdated.map(DateTime.parse(_))).sortBy(-_.getMillis).headOption

  def items = collectionEssentials.items

  def contentItems = items collect { case c: FaciaContent => c }

  def withTimeStamps = transformCards(_.withTimeStamp)

  def dateLink: Option[String] = {
    val maybeDateHeadline = customHeader flatMap  {
      case MetaDataHeader(_, _, _, dateHeadline, _) => Some(dateHeadline)
      case LoneDateHeadline(dateHeadline) => Some(dateHeadline)
      case SeriesDescriptionMetaHeader(_) => None
    }

    for {
      path <- dateLinkPath
      dateHeadline <- maybeDateHeadline
      urlFragment <- dateHeadline.urlFragment
    } yield s"$path/$urlFragment/all"
  }

  def hasShowMore = containerLayout.exists(_.hasShowMore)

  def hasDesktopShowMore = containerLayout.exists(_.hasDesktopShowMore)

  def hasMobileOnlyShowMore =
    containerLayout.exists(layout => layout.hasMobileShowMore && !layout.hasDesktopShowMore)

  /** Nasty hardcoded thing.
    *
    * TODO: change Facia Tool to have a dropdown for 'header types', one of which is default, the other CP Scott.
    *
    * Then if we end up adding more of these over time, there's an in-built mechanism for doing so. Will also mean apps
    * can consume this data if they want to.
    */
  def showCPScottHeader = Set(
    "uk/commentisfree/regular-stories",
    "au/commentisfree/regular-stories"
  ).contains(dataId)

  def addShowMoreClasses = useShowMore && containerLayout.exists(_.hasShowMore)

  def shouldLazyLoad = Switches.LazyLoadContainersSwitch.isSwitchedOn && index > 8

  def isStoryPackage = container match {
    case Dynamic(DynamicPackage) => true
    case _ => false
  }
}

object Front extends implicits.Collections {
  type TrailUrl = String

  def itemsVisible(containerDefinition: ContainerDefinition) =
    containerDefinition.slices.flatMap(_.layout.columns.map(_.numItems)).sum

  // Never de-duplicate snaps.
  def participatesInDeduplication(faciaContent: FaciaContent) = !faciaContent.embedType.isDefined

  /** Given a set of already seen trail URLs, a container type, and a set of trails, returns a new set of seen urls
    * for further de-duplication and the sequence of trails in the order that they ought to be shown for that
    * container.
    */
  def deduplicate(
    seen: Set[TrailUrl],
    container: Container,
    faciaContentList: Seq[FaciaContent]
    ): (Set[TrailUrl], Seq[FaciaContent]) = {
    container match {
      case Dynamic(dynamicContainer) =>
        /** Although Dynamic Containers participate in de-duplication, insofar as trails that appear in Dynamic
          * Containers will not be duplicated further down on the page, they themselves retain all their trails, no
          * matter what occurred further up the page.
          */
        dynamicContainer.containerDefinitionFor(
          faciaContentList.collect({ case content: CuratedContent => content }).map(Story.fromFaciaContent)
        ) map { containerDefinition =>
          (seen ++ faciaContentList
            .map(_.url)
            .take(itemsVisible(containerDefinition)), faciaContentList)
        } getOrElse {
          (seen, faciaContentList)
        }

      /** Singleton containers (such as the eyewitness one or the thrasher one) do not participate in deduplication */
      case Fixed(containerDefinition) if containerDefinition.isSingleton =>
        (seen, faciaContentList)

      case Fixed(containerDefinition) =>
        /** Fixed Containers participate in de-duplication.
          */
        val nToTake = itemsVisible(containerDefinition)
        val notUsed = faciaContentList.filter(faciaContent => !seen.contains(faciaContent.url) || !participatesInDeduplication(faciaContent))
          .distinctBy(_.url)
        (seen ++ notUsed.take(nToTake).filter(participatesInDeduplication).map(_.url), notUsed)

      case _ =>
        /** Nav lists and most popular do not participate in de-duplication at all */
        (seen, faciaContentList)
    }
  }

  def fromConfigsAndContainers(
    configs: Seq[((ContainerDisplayConfig, CollectionEssentials), Container)],
    initialContext: ContainerLayoutContext = ContainerLayoutContext.empty
  ) = {
    import scalaz.std.list._
    import scalaz.syntax.traverse._

    Front(
      configs.zipWithIndex.toList.mapAccumL(
        (Set.empty[TrailUrl], initialContext)
      ) { case ((seenTrails, context), (((config, collection), container), index)) =>
        val (newSeen, newItems) = deduplicate(seenTrails, container, collection.items)

        ContainerLayout.fromContainer(container, context, config, newItems) map {
          case (containerLayout, newContext) => ((newSeen, newContext), FaciaContainer.fromConfig(
            index,
            container,
            config.collectionConfigWithId,
            collection.copy(items = newItems),
            Some(containerLayout),
            None
          ))
        } getOrElse {
          ((newSeen, context), FaciaContainer.fromConfig(
            index,
            container,
            config.collectionConfigWithId,
            collection.copy(items = newItems),
            None,
            None
          ))
        }
      }._2.filterNot(_.items.isEmpty)
    )
  }

  def fromConfigs(configs: Seq[(CollectionConfigWithId, CollectionEssentials)]) = {
    fromConfigsAndContainers(configs.map {
      case (config, collectionEssentials) =>
        ((ContainerDisplayConfig.withDefaults(config), collectionEssentials), Container.fromConfig(config.config))
    })
  }

  def fromPressedPage(pressedPage: PressedPage,
                      initialContext: ContainerLayoutContext = ContainerLayoutContext.empty): Front = {
    import scalaz.std.list._
    import scalaz.syntax.traverse._

    Front(
      pressedPage.collections.filterNot(_.all.isEmpty).zipWithIndex.mapAccumL(
        (Set.empty[TrailUrl], initialContext)
      ) { case ((seenTrails, context), (pressedCollection, index)) =>
        val container = Container.fromPressedCollection(pressedCollection)
        val (newSeen, newItems) = deduplicate(seenTrails, container, pressedCollection.all)
        val collectionEssentials = CollectionEssentials.fromPressedCollection(pressedCollection)
        val containerDisplayConfig = ContainerDisplayConfig.withDefaults(pressedCollection.collectionConfigWithId)

        ContainerLayout.fromContainer(container, context, containerDisplayConfig, newItems) map {
          case (containerLayout, newContext) => ((newSeen, newContext), FaciaContainer.fromConfig(
            index,
            container,
            pressedCollection.collectionConfigWithId,
            collectionEssentials.copy(items = newItems),
            Some(containerLayout),
            None
          ))
        } getOrElse {
          ((newSeen, context), FaciaContainer.fromConfig(
            index,
            container,
            pressedCollection.collectionConfigWithId,
            collectionEssentials.copy(items = newItems),
            None,
            None
          ))
        }
      }._2
    )

  }

  def makeLinkedData(collections: Seq[FaciaContainer]): ItemList = {
    ItemList(
      "", // relative iri so just resolves to the base
      collections.zipWithIndex.map {
        case (collection, index) =>
          ListItem(position = index, item = Some(
            ItemList(
              "", // don't have a uri for each container
              collection.items.zipWithIndex.map {
                case (item, index) =>
                  ListItem(position = index, url = Some(item.url))
              }
            )
          ))
      }
    )
  }

}

case class Front(
  containers: Seq[FaciaContainer]
)
