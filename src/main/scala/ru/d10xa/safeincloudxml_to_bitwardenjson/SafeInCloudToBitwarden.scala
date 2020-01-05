package ru.d10xa.safeincloudxml_to_bitwardenjson

import java.util.UUID

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging

class SafeInCloudToBitwarden(bwWithFolders: Option[BitwardenDatabase])
    extends Converter[SafeInCloudDatabase, BitwardenDatabase]
    with StrictLogging {

  def checkSample(card: Card): Boolean = {
    def fieldsSetEqual(fieldNames: Set[String]): Boolean =
      card.fields
        .map(_.name)
        .toSet == fieldNames
    def containsField(
      name: String,
      value: String
    ): Boolean =
      card.fields.find(_.name == name).exists(_.text.trim == value)
    def isTwitterSample =
      card.title == "Twitter (Sample)" &&
        fieldsSetEqual(Set("Login", "Password", "Website")) &&
        containsField("Password", "Best23^Get^")
    def isPasswordSample =
      card.title == "Passport (Sample)" &&
        fieldsSetEqual(
          Set("Number", "Name", "Birthday", "Issued", "Expires")
        ) &&
        containsField("Number", "555111111") &&
        containsField("Name", "John Smith")
    def isVisaCardSample =
      card.title == "Visa Card (Sample)" &&
        fieldsSetEqual(
          Set("Number", "Owner", "Expires", "CVV", "PIN", "Blocking")
        ) &&
        containsField("Number", "5555123456789000") &&
        containsField("Owner", "John Smith")
    def isFacebookSample =
      card.title == "Facebook (Sample)" &&
        fieldsSetEqual(
          Set(
            "Login",
            "Password",
            "Website"
          )
        ) &&
        containsField("Login", "john555@gmail.com") &&
        containsField("Password", "early91*Fail*")

    def isGoogleSample =
      card.title == "Google (Sample)" &&
        fieldsSetEqual(Set("Email", "Password", "Website")) &&
        containsField("Email", "john555@gmail.com") &&
        containsField("Password", "plain79{Area{")

    def isLaptopSample =
      card.title == "Laptop (Sample)" &&
        fieldsSetEqual(Set("Login", "Password")) &&
        containsField("Password", "Save63\\apple\\")

    def isNoteSample =
      card.title == "Note (Sample)" &&
        card.fields.isEmpty &&
        card.notes.contains("This is a sample note.")

    isTwitterSample ||
    isPasswordSample ||
    isVisaCardSample ||
    isFacebookSample ||
    isGoogleSample ||
    isLaptopSample ||
    isNoteSample
  }

  def includeToExport(card: Card): Boolean = {
    val isSample = checkSample(card)
    if (card.title.contains(" (Sample)") && !isSample) {
      println(s"$card")
    }
    val isDeleted = card.deleted.getOrElse(false)
    val isTemplate = card.template.getOrElse(false)
    isTemplate || isSample || isDeleted
  }

  /**
    *
    * @return (
    *          optionally extracted field,
    *          rest fields or all fields
    *         )
    */
  def extractByName(
    fields: List[Field],
    name: String
  ): (Option[Field], List[Field]) = {
    val index = fields.indexWhere(_.name.toLowerCase == name.toLowerCase)
    if (index > -1) {
      (fields.get(index), fields.take(index) ++ fields.drop(index + 1))
    } else {
      (None, fields)
    }
  }

  def extractByNames(
    fields: List[Field],
    names: List[String]
  ): (Option[Field], List[Field]) =
    names match {
      case xs if xs.isEmpty => (None, fields)
      case name :: xs =>
        val (optField, restFields) = extractByName(fields, name)
        optField match {
          case Some(_) => (optField, restFields)
          case None => extractByNames(fields, xs)
        }
    }

  def fieldToBwUri(field: Field): BwUri = {
    require(field.text.length <= 500, s"long field name: ${field.name}")
    BwUri(uri = field.text, `match` = None)
  }

  def fieldSicToBw(field: Field): BwField = {
    // TODO loss of origin field type
    val fieldType = field.fieldType
      .map {
        case "password" | "pin" | "secret" => HiddenType
        case _ => TextType
      }
      .getOrElse(TextType)
    BwField(name = field.name, value = field.text.some, `type` = fieldType)
  }

  def bwItemType(card: Card): BwItemType =
    card.cardType match {
      case Some("note") => BwNoteItemType
      case _ => BwLoginItemType
    }

  /**
    *
    * @return (optional login object, rest fields)
    */
  def mkLogin(
    card: Card
  ): (Option[BwLogin], List[Field], List[RenamedField]) = {
    val fields0 = card.fields.toList.filter(_.text.nonEmpty)

    val (optPassword, fields1) =
      extractByNames(fields0, List("password", "code"))
    val (optLogin, fields2) =
      extractByNames(fields1, List("login", "email"))
    val (optWebsite, fields3) = extractByName(fields2, "website")

    val optLoginElem: Option[BwLogin] =
      optPassword
        .orElse(optLogin)
        .orElse(optWebsite) *>
        BwLogin(
          uris = optWebsite.map(f => Seq(fieldToBwUri(f))),
          username = optLogin.map(_.text),
          password = optPassword.map(_.text),
          totp = None
        ).some

    val renamedFields = List(
      optPassword.map(f =>
        RenamedField(cardId = card.id, from = f.name, to = "login.password")
      ),
      optLogin.map(f =>
        RenamedField(cardId = card.id, from = f.name, to = "login.username")
      ),
      optWebsite.map(f =>
        RenamedField(cardId = card.id, from = f.name, to = "login.uris.uri")
      )
    ).flatten
    // TODO cardType checked twice
    (
      optLoginElem.orElse(
        if (card.cardType != Some("note"))
          Some(BwLogin(None, None, None, None))
        else None
      ),
      fields3,
      renamedFields
    )
  }

  def convertCardToItem(
    sicLabelIdToBwFolderId: String => String,
    sicCardIdToBwItemId: String => String,
    card: Card
  ): BwItem = {
    val folderId: Option[String] = card.labelId.toList match {
      case Nil => none[String]
      case head :: Nil => head.some
      case head :: xs =>
        val otherLabels = xs.mkString(",")
        println(s"Loss labels. card: ${card.title}, labels: $otherLabels")
        head.some
    }

    val (optLoginElem, restFields, _) = mkLogin(card)

    val safeincloudMetadata = BwField(
      name = BwField.SAFE_IN_CLOUD_CARD_ID_NAME,
      value = card.id.some,
      `type` = TextType
    )

    val fields = (restFields match {
      case xs if xs.isEmpty => none
      case xs => xs.map(fieldSicToBw).some
    }).getOrElse(List.empty) :+ safeincloudMetadata

    val itemType: BwItemType = bwItemType(card)

    BwItem(
      id = sicCardIdToBwItemId(card.id),
      name = Option(card.title).filter(_.trim.nonEmpty),
      organizationId = None,
      folderId = folderId,
      `type` = itemType,
      login = optLoginElem,
      notes = card.notes,
      favorite = card.star.getOrElse(false),
      collectionIds = None,
      secureNote = itemType match {
        case BwNoteItemType => BwSecureNote().some
        case _ => None
      },
      fields = fields.some,
      identity = None,
      card = None
    )
  }

  def convert(safeInCloud: SafeInCloudDatabase): BitwardenDatabase = {
    val sicLabelsToBwFolders: Seq[(Label, BwFolder)] =
      safeInCloud.labels.toList.map { label =>
        bwWithFolders
          .map(_.folders)
          .flatMap { folders =>
            val folder = folders
              .find(_.name == label.name)
            folder.map(f => (label, f))
          }
          .getOrElse(
            (
              label,
              BwFolder(
                id = UUID.randomUUID().toString,
                name = label.name
              )
            )
          )
      }

    val sicLabelIdToBwFolderId: String => String = sicLabelsToBwFolders
      .map {
        case (label, folder) =>
          (label.id, folder.id)
      }
      .toMap
      .apply

    val sicCardIdToBwItemId: String => String = (for {
      foldersList <- bwWithFolders.toList
      item <- foldersList.items
      safeInCloudId <- item.safeInCloudCardId
      bitwardenId = item.id
    } yield (safeInCloudId, bitwardenId)).toMap
      .withDefault(_ => UUID.randomUUID().toString)

    val items =
      safeInCloud.cards
        .filterNot(includeToExport)
        .map(convertCardToItem(sicLabelIdToBwFolderId, sicCardIdToBwItemId, _))

    val totalCount = safeInCloud.cards.size
    println(s"total safeInCloud cards count: $totalCount")
    val filteredItemsCount = items.size
    println(s"filtered safeInCloud cards count: $filteredItemsCount")
    println(
      s"excluded count(samples and templates): " +
        s"${totalCount - filteredItemsCount}"
    )
    BitwardenDatabase(items = items, folders = sicLabelsToBwFolders.map(_._2))
  }
}
