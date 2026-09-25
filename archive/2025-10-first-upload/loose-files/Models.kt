package udupi.core.model

data class LanguageLink(val name:String, val code:String, val url:String)
data class Flyer(
  val title:String,
  val fileUrl:String,
  val thumbnailUrl:String? = null,
  val language: LanguageLink
)
data class Book(
  val title:String,
  val detailUrl:String,
  val languageCode:String
)
data class QuranComplex(
  val title:String,
  val description:String,
  val images:List<String>
)
