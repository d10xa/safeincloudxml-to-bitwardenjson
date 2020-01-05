package ru.d10xa.safeincloudxml_to_bitwardenjson

trait Converter[A, B] {
  def convert(a: A): B
}
