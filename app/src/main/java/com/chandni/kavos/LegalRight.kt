package com.chandni.kavos

data class LegalRight(
    val id: Int,
    val title: String,
    val category: String,
    val shortLine: String,
    val whatItMeans: String,
    val whatToDo: List<String>,
    val helplines: List<String>,
    val iconRes: Int,
    val gradientStart: Int,
    val gradientEnd: Int
)

object LegalRightsRepo {
    val ALL = listOf(
        LegalRight(
            id = 1,
            title = "Zero FIR",
            category = "Public",
            shortLine = "Any police station must register your complaint — no boundary excuses.",
            whatItMeans = "Under the Zero FIR provision, any police station in India must register an FIR, regardless of where the offence took place. They cannot refuse you saying \"this isn't our area\". The FIR is later transferred to the correct station.",
            whatToDo = listOf(
                "Walk into the nearest police station — any one of them.",
                "Insist they file a Zero FIR. Quote the term clearly.",
                "Get a copy of the FIR with the FIR number — it is your right.",
                "If they refuse, escalate to the SP/DCP, or call 112."
            ),
            helplines = listOf("112 — Emergency", "100 — Police"),
            iconRes = R.drawable.ic_law_zerofir,
            gradientStart = R.color.grad_red_a,
            gradientEnd = R.color.grad_red_b
        ),
        LegalRight(
            id = 2,
            title = "Right Against Workplace Harassment",
            category = "Workplace",
            shortLine = "POSH Act 2013 — every workplace with 10+ employees must have an ICC.",
            whatItMeans = "The Sexual Harassment of Women at Workplace (Prevention, Prohibition and Redressal) Act, 2013, mandates an Internal Complaints Committee (ICC) at every workplace with 10 or more employees. You can file written complaints, and the employer must act within strict timelines.",
            whatToDo = listOf(
                "File a written complaint to the ICC within 3 months of the incident.",
                "If your workplace has no ICC, file with the Local Complaints Committee at the District Officer.",
                "ICC must complete inquiry within 90 days.",
                "You are entitled to interim relief — transfer, leave, etc.",
                "Retaliation by employer is itself an offence."
            ),
            helplines = listOf("National Commission for Women — 7827170170", "She-Box (Govt of India) — shebox.nic.in"),
            iconRes = R.drawable.ic_law_workplace,
            gradientStart = R.color.grad_purple_a,
            gradientEnd = R.color.grad_purple_b
        ),
        LegalRight(
            id = 3,
            title = "Right to File Complaint Anytime",
            category = "Public",
            shortLine = "Police cannot refuse a complaint citing time, hour, or day.",
            whatItMeans = "There is no \"office hours\" for filing an FIR. Police are required to accept complaints 24 hours a day, 365 days a year. Refusing to register a cognizable offence is itself punishable under Section 166A of IPC.",
            whatToDo = listOf(
                "Go to any police station — day or night.",
                "If you can't physically go, you can email/write to the SP or use online FIR portals.",
                "Women have the right to be questioned only at their residence in presence of a family member or woman officer.",
                "Statement under Section 164 CrPC can be recorded before a Magistrate for protection."
            ),
            helplines = listOf("112 — Emergency", "1091 — Women Helpline"),
            iconRes = R.drawable.ic_law_complaint,
            gradientStart = R.color.grad_blue_a,
            gradientEnd = R.color.grad_blue_b
        ),
        LegalRight(
            id = 4,
            title = "Protection from Domestic Violence",
            category = "Domestic",
            shortLine = "PWDVA 2005 — covers physical, emotional, sexual, and economic abuse.",
            whatItMeans = "The Protection of Women from Domestic Violence Act, 2005 protects women in any domestic relationship — wife, mother, sister, daughter, live-in partner. It covers not just physical violence but also verbal, emotional, sexual, and economic abuse.",
            whatToDo = listOf(
                "Approach a Protection Officer (district level) — they help file the application.",
                "File a Domestic Incident Report (DIR).",
                "You can claim: protection orders, residence orders, monetary relief, custody, and compensation.",
                "Magistrate must dispose of application within 60 days.",
                "Right to reside in shared household — you cannot be evicted."
            ),
            helplines = listOf("181 — Women in Distress", "1091 — Women Helpline", "NCW — 7827170170"),
            iconRes = R.drawable.ic_law_home,
            gradientStart = R.color.grad_pink_a,
            gradientEnd = R.color.grad_pink_b
        ),
        LegalRight(
            id = 5,
            title = "Right Against Stalking & Cyber Harassment",
            category = "Cyber",
            shortLine = "Section 354D IPC + IT Act 66E, 67 — online and offline stalking is a crime.",
            whatItMeans = "Stalking — physically following, contacting, or monitoring online — is a cognizable offence under Section 354D IPC. Cyber harassment, sharing private images, or creating fake profiles falls under IT Act Sections 66E and 67. First conviction: up to 3 years jail. Repeat: up to 5 years.",
            whatToDo = listOf(
                "Take screenshots of every message, post, profile — preserve evidence.",
                "File complaint at cybercrime.gov.in — online, anonymous option available.",
                "Report fake profiles to the platform (Instagram, Facebook, etc.) immediately.",
                "Block, but don't delete — police need the trail.",
                "For physical stalking: file FIR + apply for restraining order."
            ),
            helplines = listOf("1930 — National Cyber Crime", "cybercrime.gov.in"),
            iconRes = R.drawable.ic_law_cyber,
            gradientStart = R.color.grad_teal_a,
            gradientEnd = R.color.grad_teal_b
        ),
        LegalRight(
            id = 6,
            title = "Right to Free Legal Aid",
            category = "Public",
            shortLine = "Article 39A — every woman is entitled to free legal services.",
            whatItMeans = "Under Article 39A of the Constitution and the Legal Services Authorities Act 1987, every woman has the right to free legal aid — regardless of income. This includes free lawyers, court fees waived, and assistance in drafting petitions.",
            whatToDo = listOf(
                "Approach the District Legal Services Authority (DLSA) — present in every district court.",
                "Walk-in or apply online via nalsa.gov.in.",
                "Free lawyer assigned within 48 hours for urgent cases.",
                "Lok Adalats can settle matters without court fees.",
                "Tele-Law service: free legal advice over phone — 14454."
            ),
            helplines = listOf("NALSA — 15100", "Tele-Law — 14454"),
            iconRes = R.drawable.ic_law_legal,
            gradientStart = R.color.grad_green_a,
            gradientEnd = R.color.grad_green_b
        ),
        LegalRight(
            id = 7,
            title = "Right Against Acid Attack",
            category = "Public",
            shortLine = "Section 326A IPC — minimum 10 years jail; free treatment is your right.",
            whatItMeans = "Acid attack is treated as a non-bailable offence with minimum 10 years imprisonment, extendable to life. The Supreme Court has ruled that all hospitals — government AND private — must provide free treatment, including reconstructive surgery, to acid attack victims.",
            whatToDo = listOf(
                "Get to ANY hospital immediately — they cannot refuse free treatment.",
                "FIR under Section 326A IPC (not 326).",
                "Compensation: minimum ₹3 lakhs from state govt + additional from NCW.",
                "Free reconstructive surgery covered under govt schemes.",
                "Sale of acid is regulated — report illegal sales."
            ),
            helplines = listOf("112 — Emergency", "108 — Ambulance", "NCW — 7827170170"),
            iconRes = R.drawable.ic_law_shield,
            gradientStart = R.color.grad_orange_a,
            gradientEnd = R.color.grad_orange_b
        ),
        LegalRight(
            id = 8,
            title = "Right to Privacy & Dignity",
            category = "Cyber",
            shortLine = "Section 354C IPC — voyeurism is a non-bailable offence.",
            whatItMeans = "Capturing, sharing, or publishing private images of a woman without consent is a non-bailable offence under Section 354C IPC. First conviction: 1–3 years. Repeat: 3–7 years. Sharing intimate images — even consensually captured — without consent is also covered under IT Act Section 66E.",
            whatToDo = listOf(
                "File FIR immediately — preserve every screenshot/link.",
                "Report to platform for takedown (Instagram has dedicated removal team).",
                "Cyber cell can issue takedown orders to ISPs.",
                "Identity protection — your name will not be made public in court.",
                "Compensation under Victim Compensation Scheme."
            ),
            helplines = listOf("1930 — Cyber Crime", "112 — Emergency"),
            iconRes = R.drawable.ic_law_privacy,
            gradientStart = R.color.grad_indigo_a,
            gradientEnd = R.color.grad_indigo_b
        )
    )
}
