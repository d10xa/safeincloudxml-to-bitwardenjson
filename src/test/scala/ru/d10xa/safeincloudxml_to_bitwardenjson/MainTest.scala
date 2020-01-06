package ru.d10xa.safeincloudxml_to_bitwardenjson

import java.util.UUID

import com.lucidchart.open.xtract.ParseResult
import com.lucidchart.open.xtract.XmlReader

import scala.xml.Elem

class MainTest extends TestBase {
  val emptyCard = Card(
    id = "",
    title = "",
    symbol = None,
    color = None,
    star = None,
    fields = Seq.empty,
    template = None,
    labelId = Seq.empty,
    notes = None,
    cardType = None,
    timestamp = None,
    prevstamp = None,
    deleted = None,
    websiteIcon = None,
    customIcon = None,
    image = None,
    files = Seq.empty
  )
  val emptyField = Field(
    name = "",
    fieldType = None,
    history = None,
    autofill = None,
    text = "",
    score = None,
    hash = None
  )

  def parseAndConvert(
    xml: Elem,
    bwWithFolders: Option[BitwardenDatabase]
  ): BitwardenDatabase = {
    val card: ParseResult[SafeInCloudDatabase] =
      XmlReader.of[SafeInCloudDatabase].read(xml)
    require(card.errors.isEmpty, s"has errors ${card.errors}")
    new SafeInCloudToBitwarden(bwWithFolders)
      .convert(card.toOption.get)
  }

  test("main test") {
    val elem: Elem =
      <database>
        <card
          title="Google"
          id="999"
          symbol="g"
          color="blue"
          template="false"
          star="true"
          autofill="on"
          time_stamp="1570990986443"
          prev_stamp="1570990981512"
        >
          <field name="Email" type="login">a@b.c</field>
          <field
            name="Password"
            type="password"
            score="4"
            autofill="4"
            hash="11111111111111111111111111111111"
          >123456</field>
          <field name="Password" type="password">7890</field>
          <field name="Password" type="password">abcdefg</field>
          <field name="Website" type="website">https://www.google.com</field>
        </card>
      </database>

    val bw = parseAndConvert(elem, None)
    val items = bw.items

    items.should(have).size(1)

    val item = items.head
    item.name shouldBe Some("Google")
    val loginItem = item.login.get
    loginItem.password.shouldBe(Some("123456"))
    loginItem.username.shouldBe(Some("a@b.c"))
    item.fields
      .getOrElse(Seq.empty)
      .filter(_.name == "Password")
      .flatMap(_.value)
      .toSet
      .shouldBe(Set("7890", "abcdefg"))
    loginItem.uris.get.head.uri.shouldBe("https://www.google.com")
  }

  test("folder") {
    val elem: Elem =
      <database>
        <label name="Web Accounts" id="4" type="web_accounts"></label>
        <card title="Google" id="999">
          <field name="Password" type="password">abcdefg</field>
          <label_id>4</label_id>
        </card>
      </database>
    val oldBw = BitwardenDatabase(folders =
      Seq(BwFolder(UUID.randomUUID().toString, "Web Accounts")),
      items = Seq.empty
    )
    val bw = parseAndConvert(elem, Some(oldBw))
    bw.folders should have size 1
    bw.items should have size 1
    val item = bw.items.head
    item.folderId should contain(oldBw.folders.head.id)
    bw.folders
      .find(f => item.folderId.contains(f.id))
      .get
      .name shouldBe "Web Accounts"
  }

  test("note") {
    val elem: Elem =
      <database>
      <card title="Note1" id="42" symbol="note" color="yellow" type="note">
        <notes>This is a sample note.</notes>
      </card>
    </database>
    val bw = parseAndConvert(elem, None)
    val card = bw.items.head
    card.notes shouldBe Some("This is a sample note.")
    card.`type` shouldBe BwNoteItemType
  }
}
