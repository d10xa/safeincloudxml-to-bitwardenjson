package ru.d10xa.safeincloudxml_to_bitwardenjson

import java.io.FileOutputStream
import java.nio.file.Path

import scala.xml._
import com.lucidchart.open.xtract.ParseResult
import com.lucidchart.open.xtract.XmlReader
import io.circe.Printer
import io.circe.parser.parse
import ru.d10xa.safeincloudxml_to_bitwardenjson.CommandLine.BitwardenPrintParams
import ru.d10xa.safeincloudxml_to_bitwardenjson.CommandLine.CliParams
import ru.d10xa.safeincloudxml_to_bitwardenjson.CommandLine.SafeInCloudPrintParams
import ru.d10xa.safeincloudxml_to_bitwardenjson.CommandLine.SicToBwParams

object Main {

  private val printer = Printer.noSpaces.copy(dropNullValues = true)

  def main(args: Array[String]): Unit =
    CommandLine.command.parse(args.toList) match {
      case Right(params) => run(params)
      case Left(help) => System.err.println(help)
    }

  def parseSicFile(p: Path): Option[SafeInCloudDatabase] = {
    val xml: Elem = XML.loadFile(p.toFile)
    val card: ParseResult[SafeInCloudDatabase] =
      XmlReader.of[SafeInCloudDatabase].read(xml)
    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    lazy val errorMsg = s"""has errors ${card.errors.toString()}"""
    require(
      card.errors.isEmpty,
      errorMsg
    )
    card.toOption
  }

  def parseBwFile(p: Path): Option[BitwardenDatabase] =
    parse(better.files.File(p).contentAsString)
      .flatMap(_.as[BitwardenDatabase]) match {
      case Right(v) => Some(v)
      case Left(e) =>
        e.printStackTrace()
        None
    }

  def run(cliParams: CliParams): Unit =
    cliParams match {
      case SicToBwParams(inputSic, outputBw, bwWithIds) =>
        val sicDb = parseSicFile(inputSic.toPath)
        import io.circe.syntax._
        val fos = new FileOutputStream(outputBw)
        val bwWithIdsParsed = bwWithIds
          .flatMap(f => parseBwFile(f.toPath))
        val database: BitwardenDatabase =
          new SafeInCloudToBitwarden(bwWithIdsParsed)
            .convert(sicDb.get)
        fos.write(
          printer.print(database.asJson).getBytes
        )
        fos.close()
      case BitwardenPrintParams(input, output) =>
        better.files
          .File(output)
          .write(PlainPrint.print(parseBwFile(input).get))

      case SafeInCloudPrintParams(input, output) =>
        better.files
          .File(output)
          .write(PlainPrint.print(parseSicFile(input).get))
    }

}
