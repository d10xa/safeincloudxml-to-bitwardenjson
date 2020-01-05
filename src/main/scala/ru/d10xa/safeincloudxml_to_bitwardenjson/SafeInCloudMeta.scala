package ru.d10xa.safeincloudxml_to_bitwardenjson

final case class SafeInCloudMeta(
  labels: Seq[Label],
  excludedCards: Seq[Card])

final case class RenamedField(
  cardId: String,
  from: String,
  to: String)
