# References

> **Format:** APA 7th Edition  
> **Purpose:** Academic, technical, legal, and operational references supporting the UMPooling report, architecture, and module implementations. Tagged by module coverage (`M1`–`M5`, `NFR1`–`NFR2`).

---

## 1. SDG Alignment and Climate Policy

United Nations. (2015). *Transforming our world: The 2030 Agenda for Sustainable Development* (A/RES/70/1). United Nations. https://sdgs.un.org/2030agenda [SDG 13, Target 13.3]

United Nations Framework Convention on Climate Change. (2015). *The Paris Agreement*. United Nations. https://unfccc.int/process-and-meetings/the-paris-agreement [SDG context, M5]

Universiti Malaya. (2021). *Universiti Malaya Sustainability Strategic Plan 2021–2030*. UM Sustainability Centre (UMSC). https://sustainability.um.edu.my [Institutional context, M5]

---

## 2. Emission Factors and Carbon Accounting

Ministry of Investment, Trade and Industry (MITI). (2023). *Sustainability starter kit: Practical guidance for Malaysian enterprises and SMEs*. Ministry of Investment, Trade and Industry, Malaysia. https://www.miti.gov.my [M5, primary emission factor 0.20 kg CO2e/km]

United States Environmental Protection Agency (US EPA). (2022). *Greenhouse gas inventory guidance: Direct emissions from mobile combustion sources*. EPA Center for Corporate Climate Leadership. https://www.epa.gov/climateleadership [M5, passenger vehicle baseline factor]

Department for Energy Security and Net Zero. (2024). *Greenhouse gas reporting: Conversion factors 2024 (condensed set for most users)*. GOV.UK. https://www.gov.uk/government/publications/greenhouse-gas-reporting-conversion-factors-2024 [M5, CO2 calculation formula]

Malaysian Green Technology and Climate Change Corporation (MGTC). (2023). *National greenhouse gas inventory reporting guidelines: Transport sector emissions factors*. Ministry of Natural Resources, Environment and Climate Change (NRECC). https://www.mgtc.gov.my [M5, Malaysian localized emission factors]

---

## 3. Dynamic Carpooling and Spatial Matching

Agatz, N., Erera, A., Savelsbergh, M., & Wang, X. (2012). Optimization for dynamic ride-sharing: A review. *European Journal of Operational Research*, *223*(2), 295–303. https://doi.org/10.1016/j.ejor.2012.05.028 [M2, ride-matching algorithms]

Furuhata, M., Dessouky, M., Ordóñez, F., Brunet, M.-E., Wang, X., & Koenig, S. (2013). Ridesharing: The state-of-the-art and future directions. *Transportation Research Part B: Methodological*, *57*, 28–46. https://doi.org/10.1016/j.trb.2013.08.012 [M2, M3, pooled commuter coordination]

Sinnott, R. W. (1984). Virtues of the Haversine. *Sky and Telescope*, *68*(2), 159. [M2, great-circle distance matching formula]

---

## 4. Malaysian Legal and Regulatory Framework

Laws of Malaysia. (2010). *Land Public Transport Act 2010 (Act 715)*. The Commissioner of Law Revision, Malaysia. https://www.apad.gov.my [M3, legal distinction between commercial e-hailing and private cost-sharing]

Laws of Malaysia. (2010). *Personal Data Protection Act 2010 (Act 709)*. The Commissioner of Law Revision, Malaysia. https://www.pdp.gov.my [NFR2, privacy, location data retention, and consent]

Land Public Transport Agency (APAD). (2019). *Guidelines on e-hailing operation and vehicle licensing requirements in Malaysia*. Ministry of Transport Malaysia. https://www.mot.gov.my [M3, cost-share community cap justification]

---

## 5. Software Architecture, Database, and Frameworks

JetBrains. (2024). *Kotlin programming language documentation*. JetBrains s.r.o. https://kotlinlang.org/docs/home.html [Android client, all modules]

Google. (2024). *Jetpack Compose: Modern Android UI toolkit*. Android Developers. https://developer.android.com/jetpack/compose [M1–M5, UI implementation]

Google. (2024). *Firebase Cloud Messaging (FCM) architecture and payload specification*. Google Developers. https://firebase.google.com/docs/cloud-messaging [M2, driver push notifications, NFR1]

PostGIS Project. (2024). *PostGIS 3.4 spatial database extension for PostgreSQL*. Refractions Research. https://postgis.net/documentation/ [M2, spatial queries, geography data type]

Supabase. (2024). *Supabase: The open source Firebase alternative (PostgreSQL, Row Level Security, Auth, and Edge Functions)*. Supabase Pte. Ltd. https://supabase.com/docs [Architecture, database schema, RLS policies]

Google. (2024). *Google Maps Platform Android SDK documentation*. Google Cloud. https://developers.google.com/maps/documentation/android-sdk/overview [M2, campus map rendering and pin drop]

---

## 6. Artificial Intelligence and Natural Language Processing

Google for Developers. (2024). *Gemini API documentation: Structured JSON extraction with schema validation*. Google Cloud. https://ai.google.dev/docs/gemini_api_overview [M5, natural-language ride request parser (FR5.1)]

Lewis, M., Liu, Y., Goyal, N., Ghazvininejad, M., Mohamed, A., Levy, O., Stoyanov, V., & Zettlemoyer, L. (2020). BART: Denoising sequence-to-sequence pre-training for natural language generation, translation, and comprehension. *Proceedings of the 58th Annual Meeting of the Association for Computational Linguistics*, 7871–7880. https://doi.org/10.18653/v1/2020.acl-main.703 [M5, sequence parsing foundation]

---

## 7. Usability Testing and Quality Assurance

Brooke, J. (1996). SUS: A "quick and dirty" usability scale. In P. W. Jordan, B. Thomas, I. L. McClelland, & B. Weerdmeester (Eds.), *Usability evaluation in industry* (pp. 189–194). Taylor & Francis. [M3, system usability scale benchmark]

Sauro, J., & Lewis, J. R. (2016). *Quantifying the user experience: Practical statistics for user research* (2nd ed.). Morgan Kaufmann. https://doi.org/10.1016/B978-0-12-802308-2.00001-2 [Phase 5 user testing analysis]
