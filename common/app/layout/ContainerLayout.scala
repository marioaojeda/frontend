package layout

import com.gu.facia.api.models.FaciaContent
import slices._
import scalaz.std.list._
import scalaz.syntax.traverse._

case class IndexedTrail(faciaContent: FaciaContent, index: Int)

object ContainerLayout extends implicits.Collections {
  def apply(
      sliceDefinitions: Seq[Slice],
      items: Seq[FaciaContent],
      initialContext: ContainerLayoutContext,
      config: ContainerDisplayConfig,
      mobileShowMore: MobileShowMore
  ): (ContainerLayout, ContainerLayoutContext) = {
    val indexedTrails = items.zipWithIndex.map((IndexedTrail.apply _).tupled)

    val (slices, showMore, finalContext) = sliceDefinitions.foldLeft(
      (Seq.empty[SliceWithCards], indexedTrails, initialContext)
    ) {
      case ((slicesSoFar, Nil, context), _) => (slicesSoFar, Nil, context)
      case ((slicesSoFar, trailsForUse, context), sliceDefinition) =>
        val (slice, remainingTrails, newContext) = SliceWithCards.fromItems(
          trailsForUse,
          sliceDefinition.layout,
          context,
          config.collectionConfigWithId.config,
          mobileShowMore,
          config.showSeriesAndBlogKickers
        )
        (slicesSoFar :+ slice, remainingTrails, newContext)
    }

    (ContainerLayout(slices, showMore map { case IndexedTrail(trail, index) =>
      FaciaCardAndIndex(
        index,
        FaciaCard.fromTrail(
          trail,
          config.collectionConfigWithId.config,
          ItemClasses.showMore,
          config.showSeriesAndBlogKickers
        ),
        hideUpTo = Some(Desktop)
      )
    }), finalContext)
  }

  def singletonFromContainerDefinition(
    containerDefinition: ContainerDefinition,
    config: ContainerDisplayConfig,
    items: Seq[FaciaContent]
  ) = fromContainerDefinition(
    containerDefinition,
    ContainerLayoutContext.empty,
    config,
    items
  )._1

  def fromContainerDefinition(
      containerDefinition: ContainerDefinition,
      containerLayoutContext: ContainerLayoutContext,
      config: ContainerDisplayConfig,
      items: Seq[FaciaContent]
  ) = apply(
      containerDefinition.slices,
      items,
      containerLayoutContext,
      config,
      containerDefinition.mobileShowMore
    )

  def fromContainer(
      container: Container,
      containerLayoutContext: ContainerLayoutContext,
      config: ContainerDisplayConfig,
      items: Seq[FaciaContent]
  ) =
    ContainerDefinition.fromContainer(container, items) map {
      case definition: ContainerDefinition =>
        fromContainerDefinition(definition, containerLayoutContext, config, items)
    }

  def forHtmlBlobs(sliceDefinitions: Seq[Slice], blobs: Seq[HtmlAndClasses]) = {
    val slicesWithCards = (sliceDefinitions zip sliceDefinitions.map(_.layout.columns.map(_.numItems).sum))
      .toList
      .mapAccumL(blobs) {
      case (blobsRemaining, (slice, numToConsume)) =>
        val (blobsConsumed, blobsUnconsumed) = blobsRemaining.splitAt(numToConsume)
        (blobsUnconsumed, SliceWithCards.fromBlobs(slice.layout, blobsConsumed))
    }._2

    ContainerLayout(
      slicesWithCards,
      Nil
    )
  }
}

case class ContainerLayout(
  slices: Seq[SliceWithCards],
  remainingCards: Seq[FaciaCardAndIndex]
) {
  def transformCards(f: ContentCard => ContentCard) = copy(
    slices = slices.map(_.transformCards(f)),
    remainingCards.map(cardAndIndex => cardAndIndex.transformCard(f))
  )

  def hasMobileShowMore =
    slices.flatMap(_.columns.flatMap(_.cards)).exists(_.hideUpTo.contains(Mobile))

  def hasDesktopShowMore =
    remainingCards.nonEmpty

  def hasShowMore = hasDesktopShowMore || hasMobileShowMore
}
