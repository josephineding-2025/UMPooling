// NLP Ride Request Parser Edge Function (FR5.1)
// Translates natural language commuter queries (English, Malay, Manglish) into structured ride request JSON.
// Env keys supported: GEMINI_API_KEY (primary) or OPENROUTER_API_KEY (secondary). Never hardcoded.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};

const MAX_QUERY_LENGTH = 500;

// Canonical location lexicon aligned with docs/locations.json
interface LocationEntry {
  canonicalName: string;
  type: "faculty" | "residential_college" | "off_campus_zone" | "landmark" | "transit" | "gate";
  aliases: string[];
}

const UM_LOCATIONS: LocationEntry[] = [
  // Off-Campus Student Residential Zones
  { canonicalName: "Tiara Damansara", type: "off_campus_zone", aliases: ["tiara", "tiara damansara", "kondominium tiara"] },
  { canonicalName: "Astana Damansara", type: "off_campus_zone", aliases: ["astana", "astana damansara"] },
  { canonicalName: "Prima 16", type: "off_campus_zone", aliases: ["prima 16", "prima 16 condo"] },
  { canonicalName: "Li Villas", type: "off_campus_zone", aliases: ["li villas", "livillas"] },
  { canonicalName: "Pacific Star", type: "off_campus_zone", aliases: ["pacific star", "pac star", "pacific star residency"] },
  { canonicalName: "Ryan & Miho", type: "off_campus_zone", aliases: ["ryan & miho", "ryan and miho", "ryan miho"] },
  { canonicalName: "Jaya One", type: "off_campus_zone", aliases: ["jaya one", "jaya 1"] },
  { canonicalName: "Atwater", type: "off_campus_zone", aliases: ["atwater", "atwater pj"] },
  { canonicalName: "PJ Midtown", type: "off_campus_zone", aliases: ["pj midtown", "midtown"] },
  { canonicalName: "Centrestage", type: "off_campus_zone", aliases: ["centrestage", "centre stage"] },
  { canonicalName: "University Tower", type: "off_campus_zone", aliases: ["university tower", "utower", "u tower"] },
  { canonicalName: "Seksyen 11", type: "off_campus_zone", aliases: ["section 11", "seksyen 11", "sec 11"] },
  { canonicalName: "Seksyen 17", type: "off_campus_zone", aliases: ["section 17", "seksyen 17", "sec 17", "s17"] },
  { canonicalName: "Vista Angkasa", type: "off_campus_zone", aliases: ["vista angkasa", "vista", "angkasa"] },
  { canonicalName: "KL Gateway", type: "off_campus_zone", aliases: ["kl gateway", "kl gateway mall", "gateway"] },
  { canonicalName: "Residensi Kerinchi", type: "off_campus_zone", aliases: ["residensi kerinchi", "residency kerinchi", "kerinchi"] },
  { canonicalName: "South View", type: "off_campus_zone", aliases: ["south view", "south view serviced apartments", "southview"] },
  { canonicalName: "South Link", type: "off_campus_zone", aliases: ["south link", "southlink"] },
  { canonicalName: "Novum", type: "off_campus_zone", aliases: ["novum", "novum bangsar south"] },
  { canonicalName: "Pantai Hillpark", type: "off_campus_zone", aliases: ["pantai hillpark", "hillpark", "phase 1", "phase 2", "phase 5"] },
  { canonicalName: "Bangsar South", type: "off_campus_zone", aliases: ["bangsar south", "the vertical", "the sphere"] },

  // Residential Colleges (KK1 - KK13)
  { canonicalName: "KK1", type: "residential_college", aliases: ["kk1", "kolej kediaman 1", "kolej pertama", "astaka"] },
  { canonicalName: "KK2", type: "residential_college", aliases: ["kk2", "kolej kediaman 2", "tuanku bahiyah"] },
  { canonicalName: "KK3", type: "residential_college", aliases: ["kk3", "kolej kediaman 3", "tuanku kurshiah"] },
  { canonicalName: "KK4", type: "residential_college", aliases: ["kk4", "kolej kediaman 4", "bestari"] },
  { canonicalName: "KK5", type: "residential_college", aliases: ["kk5", "kolej kediaman 5", "dayasari"] },
  { canonicalName: "KK6", type: "residential_college", aliases: ["kk6", "kolej kediaman 6", "ibnu sina"] },
  { canonicalName: "KK7", type: "residential_college", aliases: ["kk7", "kolej kediaman 7", "za'ba", "zaba"] },
  { canonicalName: "KK8", type: "residential_college", aliases: ["kk8", "kolej kediaman 8", "kinabalu"] },
  { canonicalName: "KK9", type: "residential_college", aliases: ["kk9", "kolej kediaman 9", "tun syed zahiruddin"] },
  { canonicalName: "KK10", type: "residential_college", aliases: ["kk10", "kolej kediaman 10", "tun ahmad zaidi"] },
  { canonicalName: "KK11", type: "residential_college", aliases: ["kk11", "kolej kediaman 11", "ungku aziz"] },
  { canonicalName: "KK12", type: "residential_college", aliases: ["kk12", "kolej kediaman 12", "raja dr. nazrin shah", "raja dr nazrin shah"] },
  { canonicalName: "KK13", type: "residential_college", aliases: ["kk13", "kolej kediaman 13", "kolej ketiga belas"] },

  // Faculties & Academies
  { canonicalName: "FSKTM", type: "faculty", aliases: ["fsktm", "fcsit", "computer science", "sains komputer", "fakulti sains komputer"] },
  { canonicalName: "Faculty of Engineering", type: "faculty", aliases: ["engineering", "fakulti kejuruteraan", "kejuruteraan", "engin", "foe"] },
  { canonicalName: "Faculty of Science", type: "faculty", aliases: ["science faculty", "fakulti sains", "science", "sains", "fos"] },
  { canonicalName: "Faculty of Medicine", type: "faculty", aliases: ["medicine", "fakulti perubatan", "perubatan", "fom"] },
  { canonicalName: "Faculty of Law", type: "faculty", aliases: ["law", "fakulti undang-undang", "undang-undang", "fol"] },
  { canonicalName: "Faculty of Business and Economics", type: "faculty", aliases: ["fbec", "faculty of business", "fakulti perniagaan", "perniagaan", "ekonomi", "business"] },
  { canonicalName: "Faculty of Arts and Social Sciences", type: "faculty", aliases: ["fass", "arts", "sastera", "fakulti sastera"] },
  { canonicalName: "Faculty of Languages and Linguistics", type: "faculty", aliases: ["fll", "bahasa", "linguistics", "fakulti bahasa"] },
  { canonicalName: "Faculty of Built Environment", type: "faculty", aliases: ["fbe", "alam bina", "built environment"] },
  { canonicalName: "Faculty of Education", type: "faculty", aliases: ["education", "pendidikan", "fakulti pendidikan"] },
  { canonicalName: "Faculty of Pharmacy", type: "faculty", aliases: ["pharmacy", "farmasi", "fakulti farmasi"] },
  { canonicalName: "Faculty of Dentistry", type: "faculty", aliases: ["dentistry", "pergigian", "fakulti pergigian"] },

  // Campus Landmarks & Central Facilities
  { canonicalName: "Main Library", type: "landmark", aliases: ["main library", "library", "perpustakaan utama", "perpustakaan"] },
  { canonicalName: "Za'ba Memorial Library", type: "landmark", aliases: ["zaba library", "perpustakaan peringatan za'ba"] },
  { canonicalName: "DTC", type: "landmark", aliases: ["dtc", "dewan tunku canselor", "tunku canselor"] },
  { canonicalName: "Examination Building", type: "landmark", aliases: ["examination building", "bangunan peperiksaan", "peperiksaan", "exam hall"] },
  { canonicalName: "Pusat Sukan / UM Arena", type: "landmark", aliases: ["pusat sukan", "sports centre", "um arena", "stadium"] },
  { canonicalName: "UMMC", type: "landmark", aliases: ["ummc", "ppum", "pusat perubatan universiti malaya", "hospital um"] },
  { canonicalName: "Masjid Ar-Rahman", type: "landmark", aliases: ["masjid ar-rahman", "masjid um", "masjid pantai"] },

  // Transit & Gates
  { canonicalName: "LRT KL Gateway-Universiti", type: "transit", aliases: ["lrt universiti", "lrt kl gateway", "universiti lrt", "kl gateway lrt"] },
  { canonicalName: "LRT Kerinchi", type: "transit", aliases: ["lrt kerinchi", "kerinchi lrt"] },
  { canonicalName: "MRT Phileo Damansara", type: "transit", aliases: ["mrt phileo", "phileo damansara", "phileo mrt"] },
  { canonicalName: "KL Gate", type: "gate", aliases: ["kl gate", "pintu kl", "lembah pantai gate"] },
  { canonicalName: "PJ Gate", type: "gate", aliases: ["pj gate", "pintu pj", "jalan universiti gate"] },
  { canonicalName: "Damansara Gate", type: "gate", aliases: ["damansara gate", "pintu damansara", "lingkungan budi gate"] }
];

interface ParseResult {
  origin: string | null;
  destination: string | null;
  departure_date: string | null;
  departure_time: string | null;
  seats: number;
  missing_fields: string[];
  ambiguous_fields: string[];
  confidence: number;
  raw_query: string;
}

/**
 * Normalizes an extracted location string against the UM canonical location lexicon.
 */
function normalizeToUmLocation(rawText: string | null): string | null {
  if (!rawText) return null;
  const cleaned = rawText.trim().toLowerCase();
  
  // Exact or alias match
  for (const entry of UM_LOCATIONS) {
    if (entry.canonicalName.toLowerCase() === cleaned) {
      return entry.canonicalName;
    }
    for (const alias of entry.aliases) {
      if (alias === cleaned || cleaned.includes(alias)) {
        return entry.canonicalName;
      }
    }
  }
  return rawText.trim();
}

/**
 * Heuristic rule-based fallback extractor for testing and offline resilience.
 */
function heuristicExtract(query: string, referenceDateStr: string): ParseResult {
  const lower = query.toLowerCase();
  let origin: string | null = null;
  let destination: string | null = null;
  let departureDate: string | null = null;
  let departureTime: string | null = null;
  let seats = 1;
  const missing: string[] = [];
  const ambiguous: string[] = [];

  const refDate = new Date(referenceDateStr);
  const safeRefDate = isNaN(refDate.getTime()) ? new Date() : refDate;

  // Resolve seats
  const seatMatch = lower.match(/(\d+)\s*(?:seat|pax|orang|people|kerusi)/);
  if (seatMatch) {
    seats = Math.max(1, Math.min(6, parseInt(seatMatch[1], 10)));
  }

  // Resolve date
  if (lower.includes("tomorrow") || lower.includes("esok")) {
    const tmr = new Date(safeRefDate);
    tmr.setDate(tmr.getDate() + 1);
    departureDate = tmr.toISOString().split("T")[0];
  } else if (lower.includes("today") || lower.includes("hari ini")) {
    departureDate = safeRefDate.toISOString().split("T")[0];
  } else {
    departureDate = safeRefDate.toISOString().split("T")[0];
  }

  // Resolve time
  const timeMatch = lower.match(/(?:at|by|pukul|jam|kul)?\s*(\d{1,2})(?::|\.)?(\d{2})?\s*(am|pm|pagi|petang|malam)?/);
  if (timeMatch && (timeMatch[1] || timeMatch[3])) {
    let hour = parseInt(timeMatch[1], 10);
    const minute = timeMatch[2] ? parseInt(timeMatch[2], 10) : 0;
    const meridian = timeMatch[3];

    if (meridian === "pm" || meridian === "petang" || meridian === "malam") {
      if (hour < 12) hour += 12;
    } else if (meridian === "am" || meridian === "pagi") {
      if (hour === 12) hour = 0;
    }
    if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
      departureTime = `${hour.toString().padStart(2, "0")}:${minute.toString().padStart(2, "0")}`;
    }
  }

  // Find locations
  for (const loc of UM_LOCATIONS) {
    for (const alias of loc.aliases) {
      if (lower.includes(alias)) {
        const fromIdx = lower.indexOf("from " + alias);
        const dariIdx = lower.indexOf("dari " + alias);
        const toIdx = lower.indexOf("to " + alias);
        const keIdx = lower.indexOf("ke " + alias);
        const pergiIdx = lower.indexOf("pergi " + alias);

        if (fromIdx !== -1 || dariIdx !== -1) {
          origin = loc.canonicalName;
        } else if (toIdx !== -1 || keIdx !== -1 || pergiIdx !== -1) {
          destination = loc.canonicalName;
        } else if (!origin) {
          origin = loc.canonicalName;
        } else if (!destination && origin !== loc.canonicalName) {
          destination = loc.canonicalName;
        }
      }
    }
  }

  if (!origin) missing.push("origin");
  if (!destination) missing.push("destination");
  if (!departureTime) missing.push("departure_time");

  const confidence = 0.85 - (missing.length * 0.15);

  return {
    origin,
    destination,
    departure_date: departureDate,
    departure_time: departureTime,
    seats,
    missing_fields: missing,
    ambiguous_fields: ambiguous,
    confidence: Math.max(0.0, Math.min(1.0, confidence)),
    raw_query: query,
  };
}

/**
 * Invokes Gemini or OpenRouter LLM with structured output schema.
 */
async function callLlmParser(query: string, referenceTime: string): Promise<ParseResult> {
  const geminiKey = Deno.env.get("GEMINI_API_KEY");
  const openRouterKey = Deno.env.get("OPENROUTER_API_KEY");

  const systemPrompt = `You are a strict, domain-expert carpool request parser for Universiti Malaya (UM), Malaysia.
Extract transit entities from the user query into a structured JSON response.
Reference Current Datetime: ${referenceTime} (UTC+8, Asia/Kuala_Lumpur).

Known Campus & Enclave Locations:
- Off-campus: Tiara Damansara, Astana Damansara, Prima 16, Li Villas, Pacific Star, Ryan & Miho, Jaya One, Atwater, PJ Midtown, University Tower, Seksyen 11, Seksyen 17, Vista Angkasa, KL Gateway, Residensi Kerinchi, South View, South Link, Novum, Pantai Hillpark, Bangsar South.
- Colleges: KK1 to KK13 (Kolej Kediaman 1-13).
- Faculties: FSKTM (Computer Science/FCSIT), Faculty of Engineering (Kejuruteraan), Faculty of Science (Sains), Faculty of Medicine (Perubatan), Faculty of Law, Faculty of Business and Economics (FBEc), Faculty of Arts and Social Sciences (FASS), Faculty of Languages (FLL), Faculty of Built Environment (FBE), Faculty of Education, Faculty of Pharmacy, Faculty of Dentistry.
- Landmarks: Main Library (Perpustakaan Utama), DTC (Dewan Tunku Canselor), Examination Building (Bangunan Peperiksaan), Pusat Sukan / UM Arena, UMMC (PPUM), Masjid Ar-Rahman.
- Transit: LRT KL Gateway-Universiti, LRT Kerinchi, MRT Phileo Damansara, KL Gate, PJ Gate, Damansara Gate.

RULES:
1. "origin": starting pickup point. null if not explicitly mentioned.
2. "destination": destination drop-off point. null if not explicitly mentioned.
3. "departure_date": "YYYY-MM-DD" resolved relative to reference time.
4. "departure_time": "HH:mm" in 24-hr format. null if time is not stated or too vague ("later").
5. "seats": integer passenger seat count (default 1).
6. "missing_fields": list of missing fields among ["origin", "destination", "departure_time"].
7. "ambiguous_fields": list of ambiguous or conflicting fields.
8. "confidence": float 0.0 to 1.0.
9. ZERO HALLUCINATION: NEVER make up an origin, destination, or time if not stated!

Return ONLY raw JSON with keys: origin, destination, departure_date, departure_time, seats, missing_fields, ambiguous_fields, confidence.`;

  if (geminiKey) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${geminiKey}`;
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [
          {
            role: "user",
            parts: [
              { text: `${systemPrompt}\n\nUser Query: "${query}"` },
            ],
          },
        ],
        generationConfig: {
          responseMimeType: "application/json",
          temperature: 0.1,
        },
      }),
    });

    if (!response.ok) {
      throw new Error(`Gemini API error: ${response.status} ${await response.text()}`);
    }

    const json = await response.json();
    const rawContent = json.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!rawContent) throw new Error("Empty response from Gemini LLM");

    const parsed = JSON.parse(rawContent);
    return {
      origin: normalizeToUmLocation(parsed.origin),
      destination: normalizeToUmLocation(parsed.destination),
      departure_date: parsed.departure_date ?? null,
      departure_time: parsed.departure_time ?? null,
      seats: typeof parsed.seats === "number" ? Math.max(1, Math.min(6, parsed.seats)) : 1,
      missing_fields: Array.isArray(parsed.missing_fields) ? parsed.missing_fields : [],
      ambiguous_fields: Array.isArray(parsed.ambiguous_fields) ? parsed.ambiguous_fields : [],
      confidence: typeof parsed.confidence === "number" ? parsed.confidence : 0.9,
      raw_query: query,
    };
  }

  if (openRouterKey) {
    const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${openRouterKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "google/gemini-flash-1.5",
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: query },
        ],
        response_format: { type: "json_object" },
        temperature: 0.1,
      }),
    });

    if (!response.ok) {
      throw new Error(`OpenRouter API error: ${response.status} ${await response.text()}`);
    }

    const json = await response.json();
    const rawContent = json.choices?.[0]?.message?.content;
    if (!rawContent) throw new Error("Empty response from OpenRouter LLM");

    const parsed = JSON.parse(rawContent);
    return {
      origin: normalizeToUmLocation(parsed.origin),
      destination: normalizeToUmLocation(parsed.destination),
      departure_date: parsed.departure_date ?? null,
      departure_time: parsed.departure_time ?? null,
      seats: typeof parsed.seats === "number" ? Math.max(1, Math.min(6, parsed.seats)) : 1,
      missing_fields: Array.isArray(parsed.missing_fields) ? parsed.missing_fields : [],
      ambiguous_fields: Array.isArray(parsed.ambiguous_fields) ? parsed.ambiguous_fields : [],
      confidence: typeof parsed.confidence === "number" ? parsed.confidence : 0.9,
      raw_query: query,
    };
  }

  // Fallback to rule-based parser if no API key is set
  return heuristicExtract(query, referenceTime);
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Method not allowed. Use POST." }),
      { status: 405, headers: CORS_HEADERS },
    );
  }

  try {
    let body;
    try {
      body = await req.json();
    } catch {
      return new Response(
        JSON.stringify({ error: "Invalid JSON body." }),
        { status: 400, headers: CORS_HEADERS },
      );
    }

    const { query, current_datetime } = body ?? {};

    // 1. Strict input validation
    if (!query || typeof query !== "string" || query.trim().length === 0) {
      return new Response(
        JSON.stringify({ error: "Field 'query' must be a non-empty string." }),
        { status: 400, headers: CORS_HEADERS },
      );
    }

    if (query.length > MAX_QUERY_LENGTH) {
      return new Response(
        JSON.stringify({ error: `Query exceeds maximum length of ${MAX_QUERY_LENGTH} characters.` }),
        { status: 400, headers: CORS_HEADERS },
      );
    }

    const referenceTime = typeof current_datetime === "string" && current_datetime.trim().length > 0
      ? current_datetime
      : new Date().toISOString();

    // 2. Call parser (LLM or Heuristic)
    let parsedResult: ParseResult;
    try {
      parsedResult = await callLlmParser(query.trim(), referenceTime);
    } catch (llmError) {
      console.error("LLM parser failed, falling back to heuristic:", llmError);
      parsedResult = heuristicExtract(query.trim(), referenceTime);
    }

    // 3. Post-validation: ensure missing_fields is accurate
    const missing = new Set(parsedResult.missing_fields);
    if (!parsedResult.origin) missing.add("origin");
    if (!parsedResult.destination) missing.add("destination");
    if (!parsedResult.departure_time) missing.add("departure_time");
    parsedResult.missing_fields = Array.from(missing);

    return new Response(
      JSON.stringify(parsedResult),
      { status: 200, headers: CORS_HEADERS },
    );
  } catch (err: any) {
    console.error("Unhandled error in parse-ride-request:", err);
    return new Response(
      JSON.stringify({ error: "Internal server error", details: err?.message ?? String(err) }),
      { status: 500, headers: CORS_HEADERS },
    );
  }
});
