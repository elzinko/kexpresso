package kexpresso.samples

import kexpresso.Kexpresso
import kexpresso.analyze
import kexpresso.captures
import kexpresso.email
import kexpresso.examples
import kexpresso.from
import kexpresso.ipv4
import kexpresso.isoDate
import kexpresso.kexpresso
import kexpresso.toKexpressoCode

fun main() {
    // ── 1. Build a pattern and use matches / findAll ─────────────────────────
    println("=== 1. Build + matches / findAll ===")
    val wordPattern = kexpresso {
        oneOrMore { letter() }
    }
    val order = "Espresso Latte Cappuccino"
    val drinks = wordPattern.findAll(order).map { it.value }.toList()
    println("Input    : $order")
    println("matches  : ${wordPattern.matches("Espresso")}  (\"Espresso\" is all letters)")
    println("matches  : ${wordPattern.matches("Cold Brew")}  (\"Cold Brew\" has a space)")
    println("findAll  : $drinks")
    println()

    // ── 2. Domain helper: email() ────────────────────────────────────────────
    println("=== 2. Domain helper: email() ===")
    val emailPattern = kexpresso { email() }
    val contacts = "Order from barista@coffee.shop or head@roastery.co — not just 'hello'"
    val foundEmails = emailPattern.findAll(contacts).map { it.value }.toList()
    println("Input    : $contacts")
    println("emails   : $foundEmails")
    println()

    // ── 3. Domain helper: ipv4() ─────────────────────────────────────────────
    println("=== 3. Domain helper: ipv4() ===")
    val ipPattern = kexpresso { ipv4() }
    val networkLog = "Espresso machine at 192.168.1.42, grinder at 10.0.0.7"
    val ips = ipPattern.findAll(networkLog).map { it.value }.toList()
    println("Input    : $networkLog")
    println("IPs found: $ips")
    println()

    // ── 4. Typed captures ────────────────────────────────────────────────────
    println("=== 4. Typed captures ===")
    val datePattern = kexpresso {
        capture("year")  { exactly(4) { digit() } }
        literal("-")
        capture("month") { exactly(2) { digit() } }
        literal("-")
        capture("day")   { exactly(2) { digit() } }
    }
    val roastDate = "Roast date: 2026-06-06"
    val match = datePattern.find(roastDate)
    val caps  = match?.captures
    println("Input    : $roastDate")
    println("year     : ${caps?.int("year")}")
    println("month    : ${caps?.int("month")}")
    println("day      : ${caps?.int("day")}")
    println()

    // ── 5. describe() ────────────────────────────────────────────────────────
    println("=== 5. describe() ===")
    val strictEmail = kexpresso {
        startOfText()
        email()
        endOfText()
    }
    println("Pattern  : $strictEmail")
    println("describe : ${strictEmail.describe()}")
    println()

    // ── 6. Reverse: Kexpresso.from() → describe() + toKexpressoCode() ───────
    println("=== 6. Kexpresso.from() → describe() + toKexpressoCode() ===")
    val rawDate = "\\d{4}-\\d{2}-\\d{2}"
    val parsed  = Kexpresso.from(rawDate)
    println("Raw regex  : $rawDate")
    println("describe() : ${parsed.describe()}")
    println("DSL code   :")
    println(parsed.toKexpressoCode())

    // ── 7. Generate matching examples ───────────────────────────────────────
    println("=== 7. Generate matching examples ===")
    val pinPattern = kexpresso { exactly(4) { digit() } }
    println("Pattern  : $pinPattern")
    println("examples : ${pinPattern.examples(3)}")
    println()

    // ── 8. ReDoS analysis ────────────────────────────────────────────────────
    println("=== 7. ReDoS analysis ===")
    val safePattern  = kexpresso { isoDate() }
    val riskyPattern = kexpresso { oneOrMore { oneOrMore { letter() } } }

    val safeReport  = safePattern.analyze()
    val riskyReport = riskyPattern.analyze()

    println("Safe pattern  : $safePattern")
    println("  vulnerable  : ${safeReport.isPotentiallyVulnerable}")
    println()
    println("Risky pattern : $riskyPattern")
    println("  vulnerable  : ${riskyReport.isPotentiallyVulnerable}")
    riskyReport.findings.forEach { println("  finding     : ${it.message}") }
}
