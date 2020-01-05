package ru.d10xa.safeincloudxml_to_bitwardenjson

import com.lucidchart.open.xtract.XmlReader.attribute
import com.lucidchart.open.xtract.XmlReader.seq
import com.lucidchart.open.xtract.XmlReader
import com.lucidchart.open.xtract.__
import cats.syntax.all._

final case class SafeInCloudDatabase(
  cards: Seq[Card],
  labels: Seq[Label])

final case class Card(
  id: String,
  title: String,
  symbol: Option[String],
  color: Option[String],
  star: Option[Boolean],
  fields: Seq[Field],
  template: Option[Boolean],
  labelId: Seq[String],
  notes: Option[String],
  cardType: Option[String],
  timestamp: Option[String],
  prevstamp: Option[String],
  deleted: Option[Boolean],
  websiteIcon: Option[Boolean],
  customIcon: Option[String],
  image: Option[String],
  files: Seq[SicFile])

final case class SicFile(
  name: Option[String],
  base64: Option[String])

final case class Label(
  id: String,
  name: String,
  labelType: Option[String])

final case class Field(
  name: String,
  fieldType: Option[String],
  history: Option[String],
  autofill: Option[String],
  score: Option[String],
  hash: Option[String],
  text: String)

object SafeInCloudDatabase {
  implicit val databaseXmlReader: XmlReader[SafeInCloudDatabase] = (
    (__ \ "card").read(seq[Card]),
    (__ \ "label").read(seq[Label])
  ).mapN(apply _)
  // TODO optionally delete http[s]:// and make trim
  implicit val plainPrinterSafeInCloudDatabase
    : PlainPrint[SafeInCloudDatabase] = (t: SafeInCloudDatabase) => t.cards
    .map { card =>
      ((card.title +: card.fields.filter(_.text.nonEmpty).sortBy(_.text)
        .map(f => s"${f.text}")) :+ card.notes.getOrElse(""))
        .mkString("|")
    }
    .sorted
    .mkString("\n")
}

object Field {
  implicit val fieldXmlReader: XmlReader[Field] = (
    attribute[String]("name"),
    attribute[Option[String]]("type").default(None),
    attribute[Option[String]]("history").default(None),
    (__ \ "autofill").read[Option[String]],
    attribute[Option[String]]("score").default(None),
    attribute[Option[String]]("hash").default(None),
    __.read[String]
  ).mapN(apply _)
}

object SicFile {
  implicit val sicFileXmlReader: XmlReader[SicFile] = (
    attribute[Option[String]]("name").default(None),
    __.read[Option[String]]
  ).mapN(apply _)
}

object Card {
  implicit val cardXmlReader: XmlReader[Card] = (
    attribute[String]("id"),
    attribute[String]("title"),
    attribute[Option[String]]("symbol").default(None),
    attribute[Option[String]]("color").default(None),
    attribute[Option[Boolean]]("star").default(None),
    (__ \ "field").read(seq[Field]),
    attribute[Option[Boolean]]("template").default(None),
    (__ \ "label_id").read(seq[String]),
    (__ \ "notes").read[Option[String]],
    attribute[Option[String]]("type").default(None),
    attribute[Option[String]]("time_stamp").default(None),
    attribute[Option[String]]("prev_stamp").default(None),
    attribute[Option[Boolean]]("deleted").default(None),
    attribute[Option[Boolean]]("website_icon").default(None),
    (__ \ "custom_icon").read[Option[String]].default(None),
    (__ \ "image").read[Option[String]].default(None),
    (__ \ "file").read(seq[SicFile])
  ).mapN(apply _)
}

object Label {
  implicit val labelXmlReader: XmlReader[Label] = (
    attribute[String]("id"),
    attribute[String]("name"),
    attribute[Option[String]]("type").default(None)
  ).mapN(apply _)
}
