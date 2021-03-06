package model.commercial.soulmates

import common.AkkaAgent

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

case class SoulmatesAgent(groupName: String,
                              feed: SoulmatesFeed,
                              filter: Seq[Member] => Seq[Member]) {

  private lazy val agent = AkkaAgent[Seq[Member]](Nil)

  def refresh()(implicit ec: ExecutionContext): Future[Seq[Member]] = {

    def update(freshData: Seq[Member]): Future[Seq[Member]] = {
      agent.alter { oldData =>
        if (freshData.nonEmpty) freshData
        else oldData
      }
    }

    feed.loadAds() flatMap update
  }

  def sample(): Seq[Member] = filter(agent.get())
}

object SoulmatesAgent {

  lazy val womenAgent = SoulmatesAgent("women", FemaleSoulmatesFeed, Sample.take6)
  lazy val menAgent = SoulmatesAgent("men", MaleSoulmatesFeed, Sample.take6)

  private lazy val agents = Seq(
    womenAgent,
    menAgent,
    SoulmatesAgent("brighton", BrightonSoulmatesFeed, Sample.default),
    SoulmatesAgent("northwest", NorthwestSoulmatesFeed, Sample.default),
    SoulmatesAgent("scotland", ScotlandSoulmatesFeed, Sample.default),
    SoulmatesAgent("young", YoungSoulmatesFeed, Sample.default),
    SoulmatesAgent("mature", MatureSoulmatesFeed, Sample.default),
    SoulmatesAgent("westmidlands", WestMidlandsSoulmatesFeed, Sample.default),
    SoulmatesAgent("eastmidlands", EastMidlandsSoulmatesFeed, Sample.default),
    SoulmatesAgent("yorkshire", YorkshireSoulmatesFeed, Sample.default),
    SoulmatesAgent("northeast", NortheastSoulmatesFeed, Sample.default),
    SoulmatesAgent("east", EastSoulmatesFeed, Sample.default),
    SoulmatesAgent("south", SouthSoulmatesFeed, Sample.default),
    SoulmatesAgent("southwest", SouthwestSoulmatesFeed, Sample.default),
    SoulmatesAgent("wales", WalesSoulmatesFeed, Sample.default)
  )

  def refresh()(implicit ec: ExecutionContext): Future[Seq[Seq[Member]]] =
    Future.sequence(agents map (_.refresh()))

  def sample(groupName: String): Seq[Member] = {
    agents.find(_.groupName == groupName) map (_.sample()) getOrElse Nil
  }
}

object Sample {

  def default(members: Seq[Member]): Seq[Member] = {
    if (members.isEmpty) {
      Nil
    } else {
      // we are looking for 6 random people, either
      // woman/man/woman/man/woman/man or
      // man/woman/man/woman/man/woman
      val men = members.filter(_.gender == Man)
      val women = members.filter(_.gender == Woman)
      val people = Random.shuffle(Seq(Random.shuffle(men), Random.shuffle(women)))
      people.head.zip(people(1)).flatMap { case (p1, p2) => Seq(p1, p2) }.take(6)
    }
  }

  def take6(members: Seq[Member]): Seq[Member] = Random.shuffle(members) take 6
}
