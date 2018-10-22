package edu.larsjaas

import org.json4s.JObject

package object rest {
  case class GetIndexRequest(category: Option[String] = None)
  case class GetIndexResponse(index: Seq[JObject])
  case class GetEntryRequest(id: String, category: Option[String] = None)
  case class GetEntryResponse(entry: Option[JObject])
  case class PostEntryRequest(id: String, values: JObject, category: Option[String] = None)
  case class PostEntryResponse(ok: Boolean)
  case class PatchEntryRequest(id: String, values: JObject, category: Option[String] = None)
  case class PatchEntryResponse(ok: Boolean)
  case class DeleteEntryRequest(id: String, category: Option[String] = None)
  case class DeleteEntryResponse(ok: Boolean)
  case class NewEntryRequest(id: Option[String] = None, obj: Option[JObject] = None, category: Option[String] = None)
  case class NewEntryResponse(id: String, entry: JObject, category: Option[String] = None)
}