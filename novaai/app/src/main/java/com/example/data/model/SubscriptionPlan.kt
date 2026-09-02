package com.example.data.model

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val price: String = "₺ 0,00",
    val salesChannelInfo: String = "1 Satış Kanalı",
    val description: String,
    val features: List<String>,
    val badgeLabel: String,
    val isPopular: Boolean = false,
    val websiteUrl: String = "https://brandofpaper.ikas.shop"
)

object AvailablePlans {
    val PLAN_GO = SubscriptionPlan(
        id = "nova_go",
        name = "NovaAI Go",
        price = "₺ 0,00",
        salesChannelInfo = "1 Satış Kanalı",
        description = "Hızlı ve temel yapay zeka deneyimi için başlangıç paketi.",
        features = listOf(
            "1 Satış Kanalı Entegrasyonu",
            "Qwen 3.6 27B & Vision Desteği",
            "Standart Yanıt Hızı",
            "Sınırsız Temel Sohbet"
        ),
        badgeLabel = "GO"
    )

    val PLAN_PREMIGO = SubscriptionPlan(
        id = "nova_premigo",
        name = "NovaAI PremiGo",
        price = "₺ 0,00",
        salesChannelInfo = "1 Satış Kanalı",
        description = "Gelişmiş hız ve çoklu görsel analizi bir arada sunan avantajlı plan.",
        features = listOf(
            "1 Satış Kanalı Entegrasyonu",
            "Öncelikli Qwen & Llama 3.3 Hızı",
            "Yüksek Çözünürlüklü Görsel Analizi",
            "Gelişmiş Kod & Problem Çözümü"
        ),
        badgeLabel = "PREMIGO",
        isPopular = true
    )

    val PLAN_PREMIUM = SubscriptionPlan(
        id = "nova_premium",
        name = "NovaAI Premium",
        price = "₺ 0,00",
        salesChannelInfo = "1 Satış Kanalı",
        description = "Profesyonel kullanıcılar için tam donanımlı zeka ve reasoning gücü.",
        features = listOf(
            "1 Satış Kanalı Entegrasyonu",
            "DeepSeek R1 Distill & GPT OSS 120B Erişimi",
            "Ultra Hızlı Çok Modlu Görsel Tanıma",
            "Öncelikli Sunucu Kapasitesi"
        ),
        badgeLabel = "PREMIUM"
    )

    val PLAN_PRO = SubscriptionPlan(
        id = "nova_pro",
        name = "NovaAI Pro",
        price = "₺ 0,00",
        salesChannelInfo = "1 Satış Kanalı",
        description = "En üst düzey yapay zeka performansı, maksimum kota ve özel yetenekler.",
        features = listOf(
            "1 Satış Kanalı Entegrasyonu",
            "Tüm AI Modellerine Sınırsız & En Hızlı Erişim",
            "Çift Yönlü Görsel & Doküman İnceleme",
            "VIP Özel NovaAI Yapılandırması"
        ),
        badgeLabel = "PRO"
    )

    val allPlans = listOf(
        PLAN_GO,
        PLAN_PREMIGO,
        PLAN_PREMIUM,
        PLAN_PRO
    )

    val defaultPlan = PLAN_GO

    fun findById(id: String): SubscriptionPlan {
        return allPlans.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: defaultPlan
    }
}
