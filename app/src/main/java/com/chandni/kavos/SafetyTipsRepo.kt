package com.chandni.kavos

object SafetyTipsRepo {

    val ALL: List<SafetyTip> = listOf(
        SafetyTip(
            id = "self_defense",
            title = "Self-Defense Basics",
            summary = "Quick moves that buy you the seconds you need to escape and run.",
            tagline = "Essential",
            iconRes = R.drawable.ic_self_defense,
            accentColor = R.color.tip_red,
            gradientStart = R.color.grad_red_a,
            gradientEnd = R.color.grad_red_b,
            sections = listOf(
                SafetyTipSection(
                    heading = "Strike where it hurts",
                    bullets = listOf(
                        "Aim for the eyes — even a poke breaks an attacker's grip and vision",
                        "Heel of your palm to the nose — a fist hurts you, an open palm doesn't",
                        "Knees and the groin go down with much less force than you think",
                        "Throat is fragile — a hard chop ends most fights instantly"
                    )
                ),
                SafetyTipSection(
                    heading = "Use your voice",
                    bullets = listOf(
                        "Yell low and angry, not high and scared — \"BACK OFF!\" carries further than a scream",
                        "Specific commands attract help — \"Call the police!\" not just \"help\"",
                        "Drop everyday items loudly — keys, a glass — sound makes attackers freeze"
                    )
                ),
                SafetyTipSection(
                    heading = "If you're grabbed",
                    bullets = listOf(
                        "Drop your weight — go limp and heavy, hard to drag a dead-weight body",
                        "Tuck your chin if hands go near your throat",
                        "Aim for thumb to break wrist grabs — it's the weakest finger",
                        "Run toward people, light, and shops — never toward isolation"
                    )
                ),
                SafetyTipSection(
                    heading = "Carry-along multipliers",
                    bullets = listOf(
                        "Keys laced through fingers turn a fist into a weapon",
                        "A heavy water bottle doubles as a club",
                        "Pepper spray — only useful if you can reach it in under 2 seconds, so wear it on the strap, not buried in a bag"
                    )
                )
            )
        ),
        SafetyTip(
            id = "first_aid",
            title = "First Aid Essentials",
            summary = "What you actually do in the first 5 minutes — before help arrives.",
            tagline = "Lifesaving",
            iconRes = R.drawable.ic_first_aid,
            accentColor = R.color.tip_green,
            gradientStart = R.color.grad_green_a,
            gradientEnd = R.color.grad_green_b,
            sections = listOf(
                SafetyTipSection(
                    heading = "Heavy bleeding",
                    bullets = listOf(
                        "Press firmly with any clean cloth — don't peek for at least 10 minutes",
                        "Elevate the wound above the heart if possible",
                        "If blood soaks through, add cloth on top — never remove the first layer",
                        "Tourniquet only as a last resort — note the time it went on"
                    )
                ),
                SafetyTipSection(
                    heading = "Burns",
                    bullets = listOf(
                        "Cool running water for 20 full minutes — set a timer, it feels long",
                        "No ice, no butter, no toothpaste — these all make it worse",
                        "Cover loosely with cling film or a clean plastic bag, not cotton",
                        "Hospital for any burn larger than your palm or on face/joints"
                    )
                ),
                SafetyTipSection(
                    heading = "Choking",
                    bullets = listOf(
                        "If they can cough or talk — let them, don't slap their back",
                        "Silent + clutching throat = act now: 5 hard back blows between shoulder blades",
                        "Then 5 abdominal thrusts (Heimlich) — fist above the navel, sharp inward-and-up",
                        "Alternate until the object comes out or they go unconscious"
                    )
                ),
                SafetyTipSection(
                    heading = "Fainting / unresponsive",
                    bullets = listOf(
                        "Lay them flat on their back, raise legs ~30 cm",
                        "Loosen anything tight — collar, belt, scarf",
                        "Recovery position (on side) if they're breathing but unconscious",
                        "If not breathing — call 108, start CPR: 30 chest compressions to 2 breaths"
                    )
                )
            ),
            helplines = listOf(
                "Ambulance — 108",
                "National Emergency — 112"
            )
        ),
        SafetyTip(
            id = "cab_safety",
            title = "Cab & Ride Safety",
            summary = "Verify before you get in. Share before you start. Stay aware the whole way.",
            tagline = "Travel",
            iconRes = R.drawable.ic_cab_safety,
            accentColor = R.color.tip_blue,
            gradientStart = R.color.grad_blue_a,
            gradientEnd = R.color.grad_blue_b,
            sections = listOf(
                SafetyTipSection(
                    heading = "Before you get in",
                    bullets = listOf(
                        "Number plate must match the app — read it out loud, watch the driver's face",
                        "Driver's photo must match — different person? walk away, don't apologise",
                        "Ask the driver to confirm your name — don't say it first",
                        "Take a quick photo of the number plate; send it to a guardian"
                    )
                ),
                SafetyTipSection(
                    heading = "During the ride",
                    bullets = listOf(
                        "Always sit in the back, on the side opposite the driver",
                        "Share live trip with a guardian — every ride, even short ones",
                        "Keep one earphone out — you need to hear the road",
                        "Door locked? Check yours opens. Don't trust child-locks"
                    )
                ),
                SafetyTipSection(
                    heading = "If something feels wrong",
                    bullets = listOf(
                        "Route deviates? Ask out loud: \"Why are you going this way?\"",
                        "Call someone immediately — even a fake call works to signal you're not alone",
                        "Stopped at a red light? Get out at a busy junction, walk into a shop",
                        "Use Voice SOS — \"help me\" auto-fires alert without you touching the phone"
                    )
                ),
                SafetyTipSection(
                    heading = "Auto / bus rules",
                    bullets = listOf(
                        "Always note auto number plate before boarding — say it out loud as you step in",
                        "Crowded bus: stay near the conductor, away from the back",
                        "Late-night bus stops: stand under a streetlight, near the ticket counter"
                    )
                )
            )
        ),
        SafetyTip(
            id = "emergency_numbers",
            title = "Emergency Numbers",
            summary = "Save these. Memorize the top three. They're free and they work nationwide.",
            tagline = "Helplines",
            iconRes = R.drawable.ic_emergency_call,
            accentColor = R.color.tip_orange,
            gradientStart = R.color.grad_orange_a,
            gradientEnd = R.color.grad_orange_b,
            sections = listOf(
                SafetyTipSection(
                    heading = "Memorize these three",
                    bullets = listOf(
                        "112 — One number for everything: police, fire, ambulance",
                        "1091 — Women's helpline (24×7, no questions asked)",
                        "108 — Free ambulance, anywhere in India"
                    )
                ),
                SafetyTipSection(
                    heading = "Specialised helplines",
                    bullets = listOf(
                        "100 — Police direct line",
                        "101 — Fire brigade",
                        "1098 — Childline (under 18)",
                        "181 — Women in distress (state level)",
                        "139 — Indian Railways",
                        "1930 — Cybercrime / online fraud"
                    )
                ),
                SafetyTipSection(
                    heading = "How to call faster",
                    bullets = listOf(
                        "Long-press the power button on most Androids dials emergency directly — no unlock needed",
                        "Add 112 as a contact named \"Emergency\" — even from the lock screen, contacts can show",
                        "Apple iPhone: press side button + volume 5 times — emergency SOS"
                    )
                ),
                SafetyTipSection(
                    heading = "What to say (every time)",
                    bullets = listOf(
                        "Your location — exact address or visible landmark",
                        "What happened — one short sentence",
                        "How many people involved",
                        "Don't hang up — let them end the call"
                    )
                )
            ),
            helplines = listOf(
                "All-in-one emergency — 112",
                "Women's helpline — 1091",
                "Ambulance — 108",
                "Cyber crime — 1930"
            )
        ),
        SafetyTip(
            id = "awareness",
            title = "Situational Awareness",
            summary = "Spot trouble before it reaches you — your instincts are a superpower, not paranoia.",
            tagline = "Mindset",
            iconRes = R.drawable.ic_awareness,
            accentColor = R.color.tip_purple,
            gradientStart = R.color.grad_indigo_a,
            gradientEnd = R.color.grad_indigo_b,
            sections = listOf(
                SafetyTipSection(
                    heading = "Trust the gut",
                    bullets = listOf(
                        "If a place, person, or vibe feels off — leave. You don't owe anyone politeness over your safety",
                        "Your subconscious notices micro-signals (tone, body language) before your conscious mind does",
                        "\"Am I being rude?\" is the wrong question. \"Am I safe?\" is the right one"
                    )
                ),
                SafetyTipSection(
                    heading = "Read the body language",
                    bullets = listOf(
                        "Walk with purpose — head up, shoulders back. Hesitant gait attracts predators",
                        "Eye contact in passing = \"I see you\" → not an easy target",
                        "Hands free, phone away when entering a new street — don't look distracted"
                    )
                ),
                SafetyTipSection(
                    heading = "Know your space",
                    bullets = listOf(
                        "Note exits the moment you enter any room, cab, lift, or restaurant",
                        "Sit with your back to a wall, facing the door, when you can",
                        "Late-night walks: stay on the side facing oncoming traffic — harder to be pulled into a car"
                    )
                ),
                SafetyTipSection(
                    heading = "If you think you're being followed",
                    bullets = listOf(
                        "Cross the road — if they cross too, you're not imagining it",
                        "Go INTO a shop, not toward your home — never reveal where you live",
                        "Call someone loudly with your name and location: \"I'm on MG Road near the metro, I'll be home in 10\"",
                        "Use Voice SOS or shake your phone 3× hard — KAVOS will fire SOS without a tap"
                    )
                )
            )
        ),
        SafetyTip(
            id = "digital_safety",
            title = "Digital Safety",
            summary = "Your phone, accounts, and location data are part of your physical safety.",
            tagline = "Online",
            iconRes = R.drawable.ic_law_cyber,
            accentColor = R.color.tip_blue,
            gradientStart = R.color.grad_purple_a,
            gradientEnd = R.color.grad_purple_b,
            sections = listOf(
                SafetyTipSection(
                    heading = "Lock down location sharing",
                    bullets = listOf(
                        "Turn OFF photo geotags when posting publicly — Settings → Camera → Location",
                        "Live location to friends only, never to public groups or stories",
                        "Find My Device must be ON — lets you wipe your phone remotely if it's stolen"
                    )
                ),
                SafetyTipSection(
                    heading = "Phone hygiene",
                    bullets = listOf(
                        "PIN, not patterns — patterns leave smudge trails attackers can read",
                        "Disable lock-screen notifications for banking, OTP, messaging apps",
                        "Secret-camera scan in unfamiliar lodgings — turn off all lights, look for red/green LED dots"
                    )
                ),
                SafetyTipSection(
                    heading = "Online stranger danger",
                    bullets = listOf(
                        "Never share live location with someone you've only met online",
                        "Reverse-search profile photos before trusting — fakes show up instantly",
                        "First in-person meeting: public place, daytime, share the plan + their photo with a guardian"
                    )
                ),
                SafetyTipSection(
                    heading = "If something happens",
                    bullets = listOf(
                        "Screenshot everything — chats, profile, phone numbers — before blocking",
                        "Cybercrime helpline: 1930 (for fraud / harassment / stalking)",
                        "cybercrime.gov.in lets you file an online complaint with evidence"
                    )
                )
            ),
            helplines = listOf(
                "Cyber crime — 1930",
                "Women in distress — 181"
            )
        )
    )
}
