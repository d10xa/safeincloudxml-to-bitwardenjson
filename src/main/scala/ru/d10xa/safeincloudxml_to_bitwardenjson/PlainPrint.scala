package ru.d10xa.safeincloudxml_to_bitwardenjson

trait PlainPrint[T] {
  def plain(t: T): String
}

object PlainPrint {
  def print[T: PlainPrint](t: T): String = {
    implicitly[PlainPrint[T]].plain(t)
  }
}
