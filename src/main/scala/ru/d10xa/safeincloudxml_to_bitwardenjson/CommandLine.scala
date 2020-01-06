package ru.d10xa.safeincloudxml_to_bitwardenjson

import java.io.File
import java.nio.file.Path

import com.monovore.decline._
import cats.implicits._

object CommandLine {

  sealed trait CliParams
  final case class SicToBwParams(
    inputSic: File,
    outputBw: File,
    bwWithIds: Option[File])
      extends CliParams
  sealed trait PlainPrintParams extends CliParams
  final case class BitwardenPrintParams(
    input: Path,
    output: Path)
      extends PlainPrintParams
  final case class SafeInCloudPrintParams(
    input: Path,
    output: Path)
      extends PlainPrintParams

  private val sicToBw: Command[SicToBwParams] =
    Command[SicToBwParams](
      "sic-to-bw",
      "convert SafeInCloud.xml to bitwarden.json"
    )(
      (
        Opts
          .option[Path]("input", "sic input xml")
          .map(_.toFile)
          .validate("expect input is file")(_.isFile),
        Opts
          .option[Path]("output", "bw output json")
          .map(_.toFile),
        Opts
          .option[Path](
            "bw-with-ids",
            "bw backup with folders (use ids from backup)"
          )
          .orNone
          .map(_.map(_.toFile))
      ).tupled.map((SicToBwParams.apply _).tupled)
    )

  private val optsBw: Opts[BitwardenPrintParams] = (
    Opts
      .option[Path]("input-bw", "bitwarden json file"),
    Opts
      .option[Path]("output", "result output file")
  ).tupled
    .map { case (input, output) => BitwardenPrintParams(input, output) }

  private val optsSic: Opts[SafeInCloudPrintParams] = (
    Opts
      .option[Path]("input-sic", "SafeInCloud xml file"),
    Opts
      .option[Path]("output", "result output file")
  ).tupled
    .map { case (input, output) => SafeInCloudPrintParams(input, output) }

  private val plainPrint: Command[PlainPrintParams] =
    Command[PlainPrintParams](
      "plain-print",
      "print database in plain comparable format"
    )(
      optsBw.widen[PlainPrintParams]
        .orElse(optsSic).widen[PlainPrintParams]
    )

  val command: Command[CliParams] =
    Command[CliParams]("", "")(
      Opts.subcommand(sicToBw).orElse(Opts.subcommand(plainPrint))
    )

}
