package com.example.model

import java.util.Locale

enum class TranslateLanguage(
    val code: String,
    val bcp47: String,
    val persianName: String,
    val nativeName: String,
    val englishName: String,
    val shortBadge: String,
    val isRtl: Boolean,
    val locale: Locale,
    val sampleGreeting: String,
    val sampleGreetingPersian: String
) {
    PERSIAN(
        code = "fa",
        bcp47 = "fa-IR",
        persianName = "فارسی",
        nativeName = "فارسی",
        englishName = "Persian (Farsi)",
        shortBadge = "FA",
        isRtl = true,
        locale = Locale.forLanguageTag("fa-IR"),
        sampleGreeting = "سلام، خیلی خوشبختم از آشنایی با شما. لطفاً کمی آهسته‌تر صحبت کنید.",
        sampleGreetingPersian = "سلام، خیلی خوشبختم از آشنایی با شما."
    ),
    ENGLISH(
        code = "en",
        bcp47 = "en-US",
        persianName = "انگلیسی",
        nativeName = "English",
        englishName = "English",
        shortBadge = "EN",
        isRtl = false,
        locale = Locale.US,
        sampleGreeting = "Hello! I am using a simultaneous translator. When you speak into the phone, I hear Persian in my earbud.",
        sampleGreetingPersian = "سلام! من از مترجم همزمان استفاده می‌کنم. وقتی با گوشی صحبت می‌کنید، من ترجمه فارسی را در هدفونم می‌شنوم."
    ),
    RUSSIAN(
        code = "ru",
        bcp47 = "ru-RU",
        persianName = "روسی",
        nativeName = "Русский",
        englishName = "Russian",
        shortBadge = "RU",
        isRtl = false,
        locale = Locale.forLanguageTag("ru-RU"),
        sampleGreeting = "Здравствуйте! Говорите, пожалуйста, в телефон — я сразу услышу перевод на персидский в наушнике.",
        sampleGreetingPersian = "سلام! لطفاً در گوشی صحبت کنید — من بلافاصله ترجمه فارسی را در هدفونم می‌شنوم."
    ),
    TURKISH(
        code = "tr",
        bcp47 = "tr-TR",
        persianName = "ترکی استانبولی",
        nativeName = "Türkçe",
        englishName = "Turkish",
        shortBadge = "TR",
        isRtl = false,
        locale = Locale.forLanguageTag("tr-TR"),
        sampleGreeting = "Merhaba! Lütfen telefona konuşun, kulaklığımdan anında Farsça çevirisini duyuyorum.",
        sampleGreetingPersian = "سلام! لطفاً در گوشی صحبت کنید، من از هدفونم بلافاصله ترجمه فارسی را می‌شنوم."
    ),
    SPANISH(
        code = "es",
        bcp47 = "es-ES",
        persianName = "اسپانیایی",
        nativeName = "Español",
        englishName = "Spanish",
        shortBadge = "ES",
        isRtl = false,
        locale = Locale.forLanguageTag("es-ES"),
        sampleGreeting = "¡Hola! Por favor hable al teléfono; escucho la traducción al persa directamente en mi auricular.",
        sampleGreetingPersian = "سلام! لطفاً با گوشی صحبت کنید؛ من ترجمه فارسی را مستقیماً در هدفونم می‌شنوم."
    ),
    ARABIC(
        code = "ar",
        bcp47 = "ar-SA",
        persianName = "عربی",
        nativeName = "العربية",
        englishName = "Arabic",
        shortBadge = "AR",
        isRtl = true,
        locale = Locale.forLanguageTag("ar-SA"),
        sampleGreeting = "مرحباً! تفضل بالتحدث في الهاتف، فأنا أسمع الترجمة الفارسية فوراً عبر سماعة الأذن.",
        sampleGreetingPersian = "سلام! لطفاً در گوشی صحبت کنید، من فوراً ترجمه فارسی را از طریق هدفون می‌شنوم."
    );

    companion object {
        val partnerLanguages: List<TranslateLanguage> = listOf(
            ENGLISH,
            RUSSIAN,
            TURKISH,
            SPANISH,
            ARABIC
        )

        fun fromCode(code: String): TranslateLanguage? {
            val normalized = code.trim().lowercase()
            return entries.find {
                it.code == normalized ||
                    it.bcp47.lowercase() == normalized ||
                    it.englishName.lowercase().contains(normalized) ||
                    it.persianName == normalized
            }
        }
    }
}

/**
 * Defines which direction in the dual-channel physical setup is active:
 * - HOST_EARBUD_TO_SPEAKER: User wears earbud & speaks Persian -> Translated to Partner's language -> Played on Phone Speaker.
 * - PARTNER_PHONE_TO_EARBUD: Partner holds phone & speaks Foreign language -> Translated to Persian -> Played in User's Earbud / Earpiece.
 * - AUTO_DETECT_LIVE: Gemini Live listens to speech, identifies whether it is Persian or the Partner's language,
 *   translates to the opposite side, and automatically routes audio to the correct hardware output!
 */
enum class ConversationChannel(
    val persianTitle: String,
    val persianRouteBadge: String
) {
    HOST_EARBUD_TO_SPEAKER(
        persianTitle = "شما (فارسی ⬅ اسپیکر مخاطب)",
        persianRouteBadge = "پخش از اسپیکر گوشی برای طرف مقابل"
    ),
    PARTNER_PHONE_TO_EARBUD(
        persianTitle = "مخاطب (گوشی ⬅ هدفون شما)",
        persianRouteBadge = "پخش در هدفون / گوشی کنار گوش شما"
    ),
    AUTO_DETECT_LIVE(
        persianTitle = "جمینای لایو (تشخیص خودکار دوطرفه)",
        persianRouteBadge = "تفکیک خودکار هدفون و اسپیکر"
    )
}

enum class AudioRoutingMode(
    val title: String,
    val subtitle: String
) {
    SPLIT_EARBUD_SPEAKER(
        title = "تفکیک هدفون و اسپیکر (پیشنهادی)",
        subtitle = "صدای شما از اسپیکر گوشی برای مخاطب • صدای مخاطب در هدفون/کپسول گوشی شما"
    ),
    SPEAKER_ONLY(
        title = "پخش دوطرفه از اسپیکر گوشی",
        subtitle = "مناسب قرار دادن گوشی روی میز بین دو نفر بدون هدفون"
    ),
    EARBUD_ONLY(
        title = "پخش کامل در هدفون / کپسول گوشی",
        subtitle = "مناسب گوش دادن خصوصی به تمام ترجمه‌ها"
    )
}

enum class TranslationTone(
    val persianLabel: String,
    val promptInstruction: String
) {
    CONVERSATIONAL(
        persianLabel = "محاوره‌ای و طبیعی",
        promptInstruction = "Use natural, warm, everyday conversational phrasing suitable for live spoken dialogue."
    ),
    FORMAL(
        persianLabel = "رسمی و محترمانه",
        promptInstruction = "Use polite, respectful, formal register appropriate for official or diplomatic situations."
    ),
    TRAVEL(
        persianLabel = "سفر، هتل و فرودگاه",
        promptInstruction = "Optimize terminology for travel, transit, hotels, dining, and directions."
    ),
    BUSINESS(
        persianLabel = "تجاری و جلسات کاری",
        promptInstruction = "Use clear, professional business and negotiation terminology."
    ),
    MEDICAL(
        persianLabel = "پزشکی و اورژانسی",
        promptInstruction = "Use accurate, unambiguous medical and healthcare terminology."
    )
}

data class ConversationTurn(
    val id: String,
    val timestampMillis: Long,
    val channel: ConversationChannel,
    val sourceLanguage: TranslateLanguage,
    val targetLanguage: TranslateLanguage,
    val originalText: String,
    val translatedText: String,
    val phoneticGuide: String,
    val culturalNote: String? = null,
    val routedToDescription: String,
    val isBookmarked: Boolean = false
)

data class QuickPhrase(
    val id: String,
    val categoryPersian: String,
    val persianText: String,
    val translations: Map<TranslateLanguage, String>,
    val phonetics: Map<TranslateLanguage, String>
)

object PresetPhrasesRepository {
    val phrases: List<QuickPhrase> = listOf(
        QuickPhrase(
            id = "setup_explain",
            categoryPersian = "شروع مکالمه",
            persianText = "سلام! لطفاً با این گوشی به زبان خودتان صحبت کنید؛ من ترجمه فارسی را در هدفونم می‌شنوم.",
            translations = mapOf(
                TranslateLanguage.ENGLISH to "Hello! Please speak into this phone in your language; I will hear the Persian translation in my earbud.",
                TranslateLanguage.RUSSIAN to "Здравствуйте! Пожалуйста, говорите в этот телефон на своём языке — я услышу перевод на персидский в наушнике.",
                TranslateLanguage.TURKISH to "Merhaba! Lütfen bu telefona kendi dilinizde konuşun; Farsça çevirisini kulaklığımdan duyacağım.",
                TranslateLanguage.SPANISH to "¡Hola! Por favor, hable en su idioma a este teléfono; escucharé la traducción al persa en mi auricular.",
                TranslateLanguage.ARABIC to "مرحباً! يرجى التحدث إلى هذا الهاتف بلغتك؛ سأسمع الترجمة الفارسية في سماعة الأذن الخاصة بي."
            ),
            phonetics = mapOf(
                TranslateLanguage.ENGLISH to "هِلو! پلیز اسپیک اینتو دیس فون این یور لَنگوئج...",
                TranslateLanguage.RUSSIAN to "زدراست‌ووی‌تیه! پاژالوستا گَواری‌تیه و اِتات تِلفون...",
                TranslateLanguage.TURKISH to "مَرحابا! لطفاً بو تِلفونا کِندی دیلینیزده کونوشون...",
                TranslateLanguage.SPANISH to "اولا! پور فاور آبله اِن سو ایدیوما آ اِسته تِلفونو...",
                TranslateLanguage.ARABIC to "مَرحَباً! یُرجی التَحَدُث إلی هٰذا الهاتِف بِلُغَتِک..."
            )
        ),
        QuickPhrase(
            id = "speak_slower",
            categoryPersian = "کنترل گفتگو",
            persianText = "ممنون می‌شوم کمی آهسته‌تر صحبت کنید تا ترجمه دقیق‌تر انجام شود.",
            translations = mapOf(
                TranslateLanguage.ENGLISH to "I would appreciate it if you could speak a bit more slowly for a more accurate translation.",
                TranslateLanguage.RUSSIAN to "Буду признателен, если вы будете говорить чуть медленнее для более точного перевода.",
                TranslateLanguage.TURKISH to "Daha doğru bir çeviri için 조금 daha yavaş konuşabilirseniz çok sevinirim.",
                TranslateLanguage.SPANISH to "Le agradecería que hablara un poco más despacio para una traducción más precisa.",
                TranslateLanguage.ARABIC to "سأكون ممتناً لو تحدثت ببطء أكثر قليلاً للحصول على ترجمة أدق."
            ),
            phonetics = mapOf(
                TranslateLanguage.ENGLISH to "آی وود اپریشی‌ایت ایت ایف یو کود اسپیک اِ بیت مور اسلولی...",
                TranslateLanguage.RUSSIAN to "بودو پریزناتیلِن یِسلی وی بودیتیه گَواریت چوت مِدلینیه...",
                TranslateLanguage.TURKISH to "داها دوئرو بیر چِویری ایچین بیراز داها یاواش کونوشابیلیرسِنیز...",
                TranslateLanguage.SPANISH to "له آگرادِسِریا که آبلارا اون پوکو ماس دِسپاسیو...",
                TranslateLanguage.ARABIC to "سَأکونُ مُمتَناً لَو تَحَدَثتَ بِبُطءٍ أکثَر قَلیلاً..."
            )
        ),
        QuickPhrase(
            id = "directions_help",
            categoryPersian = "سفر و آدرس",
            persianText = "ببخشید، چطور می‌توانم به نزدیک‌ترین ایستگاه مترو یا تاکسی بروم؟",
            translations = mapOf(
                TranslateLanguage.ENGLISH to "Excuse me, how can I get to the nearest metro station or taxi stand?",
                TranslateLanguage.RUSSIAN to "Извините, как мне добраться до ближайшей станции метро или стоянки такси?",
                TranslateLanguage.TURKISH to "Affedersiniz, en yakın metro istasyonuna veya taksi durağına nasıl gidebilirim?",
                TranslateLanguage.SPANISH to "Disculpe, ¿cómo puedo llegar a la estación de metro o parada de taxis más cercana?",
                TranslateLanguage.ARABIC to "عفواً، كيف يمكنني الوصول إلى أقرب محطة مترو أو موقف سيارات أجرة؟"
            ),
            phonetics = mapOf(
                TranslateLanguage.ENGLISH to "اکسکیوز می، هاو کَن آی گِت تو دِ نیرِست مترو استیشن...",
                TranslateLanguage.RUSSIAN to "ایزوینی‌تیه، کاک منیه دابراتسا دو بلیژایشِی استانتسی مترو...",
                TranslateLanguage.TURKISH to "آفِدِرسینیز، اِن یاکین مترو ایستاسیونونا ناسیل گیدِبیلیریم...",
                TranslateLanguage.SPANISH to "دیسکولپه، کومو پوئدو یِگار آ لا اِستاسیون ده مترو...",
                TranslateLanguage.ARABIC to "عَفواً، کَیفَ یُمکِنُني الوُصول إلی أقرَب مَحَطَّة مترو..."
            )
        ),
        QuickPhrase(
            id = "business_price",
            categoryPersian = "خرید و تجارت",
            persianText = "قیمت نهایی این محصول یا خدمات چقدر است و آیا امکان پرداخت با کارت وجود دارد؟",
            translations = mapOf(
                TranslateLanguage.ENGLISH to "What is the final price for this product or service, and can I pay by card?",
                TranslateLanguage.RUSSIAN to "Какова окончательная цена за этот товар или услугу, и можно ли оплатить картой?",
                TranslateLanguage.TURKISH to "Bu ürün veya hizmetin son fiyatı nedir ve kartla ödeme yapabilir miyim?",
                TranslateLanguage.SPANISH to "¿Cuál es el precio final de este producto o servicio y puedo pagar con tarjeta?",
                TranslateLanguage.ARABIC to "ما هو السعر النهائي لهذا المنتج أو الخدمة، وهل يمكنني الدفع بالبطاقة؟"
            ),
            phonetics = mapOf(
                TranslateLanguage.ENGLISH to "وات ایز دِ فاینال پرایس فور دیس پروداکت، اند کَن آی پی بای کارد؟",
                TranslateLanguage.RUSSIAN to "کاکاوا آکانچاتیلنایا تسِنا زا اِتات تاوار، ای موژنا لی آپلاتیت کارتوی؟",
                TranslateLanguage.TURKISH to "بو اورون ویا هیزمِتین سون فیتی نِدیر و کارتلا اودِمه یاپابیلیر مییم؟",
                TranslateLanguage.SPANISH to "کوال اِس ال پرِسیو فینال ده اِسته پرودوکتو ای پوئدو پاگار کون تارخِتا؟",
                TranslateLanguage.ARABIC to "ما هُوَ السِعرُ النِهائي لِهٰذا المُنتَج، وَهَل یُمکِنُني الدَفع بِالبِطاقَة؟"
            )
        ),
        QuickPhrase(
            id = "medical_help",
            categoryPersian = "پزشکی و ضروری",
            persianText = "من به داروخانه یا پزشک نیاز دارم؛ لطفاً راهنمایی‌ام کنید.",
            translations = mapOf(
                TranslateLanguage.ENGLISH to "I need a pharmacy or a doctor; please help me find one.",
                TranslateLanguage.RUSSIAN to "Мне нужна аптека или врач, пожалуйста, помогите мне.",
                TranslateLanguage.TURKISH to "Bir eczaneye veya doktora ihtiyacım var; lütfen bana yardımcı olun.",
                TranslateLanguage.SPANISH to "Necesito una farmacia o un médico; por favor, ayúdeme a encontrar uno.",
                TranslateLanguage.ARABIC to "أحتاج إلى صيدلية أو طبيب؛ أرجو مساعدتي."
            ),
            phonetics = mapOf(
                TranslateLanguage.ENGLISH to "آی نید اِ فارماسی اور اِ داکتر؛ پلیز هلپ می فایند وان.",
                TranslateLanguage.RUSSIAN to "منیه نوژنا آپتِکا ایلی وراچ، پاژالوستا پاماگیتیه منیه.",
                TranslateLanguage.TURKISH to "بیر اِجزانِیه ویا دوکتورا ایحتیاجیم وار؛ لطفاً بانا یاردیمجی اولون.",
                TranslateLanguage.SPANISH to "نِسِسیتو اونا فارماسیا او اون مِدیکو؛ پور فاور آیودِمه.",
                TranslateLanguage.ARABIC to "أحتاجُ إلی صَیدَلیَّة أو طَبیب؛ أرجو مُساعَدَتي."
            )
        )
    )
}
