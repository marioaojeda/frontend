package slices

import play.api.libs.json.Json

object IdsJson {
  private val DynamicGroups = Seq(
    "standard",
    "big",
    "very big",
    "huge"
  )

  private val DynamicPackage = Seq(
    "standard",
    "snap"
  )

  def fixed(ids: Seq[String]) = Json.stringify(Json.toJson(
    ids.map(id => ContainerJsonConfig(id, None))
  ))

  def dynamic(ids: Seq[String]) = Json.stringify(Json.toJson(
    ids.map(id => 
      if (id == "dynamic/package") {
        ContainerJsonConfig(id, Some(DynamicPackage))
      } else {
        ContainerJsonConfig(id, Some(DynamicGroups))
      }
    )
  ))
}