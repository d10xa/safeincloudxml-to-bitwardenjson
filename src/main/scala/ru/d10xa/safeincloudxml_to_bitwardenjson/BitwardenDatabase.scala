package ru.d10xa.safeincloudxml_to_bitwardenjson

import io.circe.generic.semiauto._
import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import io.circe.syntax._
import cats.implicits._

final case class BitwardenDatabase(
  folders: Seq[BwFolder],
  items: Seq[BwItem])
final case class BwUri(
  uri: String,
  `match`: Option[Int])
final case class BwFolder(
  id: String,
  name: String)
final case class BwField(
  name: String,
  value: Option[String],
  `type`: FieldType)

/**
  * [[https://github.com/bitwarden/jslib/blob/master/src/enums/fieldType.ts]]
  */
sealed trait FieldType
object FieldType {
  implicit val fieldTypeCirceEncoder: Encoder[FieldType] =
    Encoder.instance {
      case TextType => 0.asJson
      case HiddenType => 1.asJson
      case BooleanType => 2.asJson
    }
  implicit val fieldTypeCirceDecoder: Decoder[FieldType] =
    Decoder { hcursor =>
      hcursor.as[Int].map {
        case 0 => TextType
        case 1 => HiddenType
        case 2 => BooleanType
      }
    }
}
object TextType extends FieldType
object HiddenType extends FieldType
object BooleanType extends FieldType
final case class BwLogin(
  uris: Option[Seq[BwUri]],
  username: Option[String],
  password: Option[String],
  // safeincloud totp is not the same as bitwarden totp
  totp: Option[String])

/**
  * [[https://github.com/bitwarden/jslib/blob/master/src/enums/cipherType.ts]]
  * [[https://github.com/bitwarden/jslib/blob/master/src/enums/secureNoteType.ts]]
  */
@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class BwSecureNote(`type`: Int = 0)
final case class BwIdentity(
  title: Option[String],
  firstName: Option[String],
  middleName: Option[String],
  lastName: Option[String],
  address1: Option[String],
  address2: Option[String],
  address3: Option[String],
  city: Option[String],
  state: Option[String],
  postalCode: Option[String],
  country: Option[String],
  company: Option[String],
  email: Option[String],
  phone: Option[String],
  ssn: Option[String],
  username: Option[String],
  passportNumber: Option[String],
  licenseNumber: Option[String])
final case class BwItem(
  id: String,
  name: Option[String],
  organizationId: Option[String],
  folderId: Option[String],
  `type`: BwItemType,
  login: Option[BwLogin],
  notes: Option[String],
  favorite: Boolean,
  collectionIds: Option[Seq[String]],
  secureNote: Option[BwSecureNote],
  fields: Option[Seq[BwField]],
  identity: Option[BwIdentity],
  card: Option[BwCard]) {
  def findFieldByName(name: String): Option[BwField] =
    fields.flatMap(fs => fs.find(_.name === name))
  def safeInCloudCardId: Option[String] =
    fields.flatMap(seq =>
      seq.find(_.name === BwField.SAFE_IN_CLOUD_CARD_ID_NAME).flatMap(_.value)
    )
}

sealed trait BwItemType
object BwIdentityItemType extends BwItemType
object BwCardItemType extends BwItemType
object BwNoteItemType extends BwItemType
object BwLoginItemType extends BwItemType
object BwItemType {
  implicit val bwItemTypeCirceEncoder: Encoder[BwItemType] =
    Encoder.instance {
      case BwLoginItemType => 1.asJson
      case BwNoteItemType => 2.asJson
      case BwCardItemType => 3.asJson
      case BwIdentityItemType => 4.asJson
    }
  implicit val bwItemTypeCirceDecoder: Decoder[BwItemType] =
    Decoder { hcursor =>
      hcursor.as[Int].map {
        case 1 => BwLoginItemType
        case 2 => BwNoteItemType
        case 3 => BwCardItemType
        case 4 => BwIdentityItemType
      }
    }
}

final case class BwCard(
  cardholderName: Option[String],
  brand: Option[String],
  number: Option[String],
  expMonth: Option[String],
  expYear: Option[String],
  code: Option[String])

object BitwardenDatabase {
  implicit val circeCodecX: Codec[BitwardenDatabase] = deriveCodec
  implicit val plainPrinterBitwardenDatabase: PlainPrint[BitwardenDatabase] =
    (t: BitwardenDatabase) =>
      t.items
        .map { item =>
          val fields: Seq[String] = item.fields
            .map(_.filter(f => f.name =!= BwField.SAFE_IN_CLOUD_CARD_ID_NAME))
            .getOrElse(Seq.empty[BwField])
            .map(i => s"${i.value.getOrElse("None")}")
          val loginFields: List[String] = item.login.toList
            .flatMap { l =>
              l.username.toList ++
                l.password.toList ++
                l.uris.toList.flatMap(_.map(_.uri))
            }

          val nameStr = item.name.getOrElse("None")
          val fieldsStrs = (fields ++ loginFields).sorted
          val noteStr = item.notes.getOrElse("")
          (nameStr +: fieldsStrs :+ noteStr).mkString("|")
        }
        .sorted
        .mkString("\n")
}

object BwUri {
  implicit val circeCodecBwUri: Codec[BwUri] = deriveCodec
}
object BwFolder {
  implicit val circeCodecBwFolder: Codec[BwFolder] = deriveCodec
}
object BwLogin {
  implicit val circeCodecBwLogin: Codec[BwLogin] = deriveCodec
}
object BwSecureNote {
  implicit val circeCodecBwSecureNote: Codec[BwSecureNote] = deriveCodec
}
object BwField {
  val SAFE_IN_CLOUD_CARD_ID_NAME = "safeincloud_card_id"
  implicit val circeCodecBwField: Codec[BwField] = deriveCodec
  def safeInCloudCardId(id: String): BwField = BwField(
    name = SAFE_IN_CLOUD_CARD_ID_NAME,
    value = Some(id),
    `type` = TextType
  )
}
object BwIdentity {
  implicit val circeCodecBwIdentity: Codec[BwIdentity] = deriveCodec
}
object BwCard {
  implicit val circeCodecBwCard: Codec[BwCard] = deriveCodec
}
object BwItem {
  implicit val circeCodecBwItem: Codec[BwItem] = deriveCodec
}
