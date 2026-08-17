package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.GalleryItem
import com.example.data.model.OrderEntity
import com.example.data.model.ProductItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GalleryItem::class, ProductItem::class, OrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FishGardenDatabase : RoomDatabase() {

    abstract fun galleryDao(): GalleryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: FishGardenDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FishGardenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FishGardenDatabase::class.java,
                    "fish_garden_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: FishGardenDatabase) {
            val galleryDao = database.galleryDao()
            val productDao = database.productDao()
            val orderDao = database.orderDao()

            if (galleryDao.getCount() == 0) {
                galleryDao.insertAll(
                    listOf(
                        GalleryItem(
                            title = "Amazonian Biotope Aquascape",
                            category = "Planted Aquascapes",
                            description = "High-tech lush planted aquarium featuring wild driftwood branches, dwarf hairgrass carpet, and vibrant school of cardinal tetras.",
                            imageUrl = "img_hero_aquarium",
                            tankSpecs = "120x50x50cm Rimless 300L • ADA Aqua Soil • Twinstar 900S LED • CO2 Pressurized 3bps",
                            floraFauna = "Cardinal Tetras, Otocinclus, Amano Shrimp, Rotala Rotundifolia, Monte Carlo",
                            likesCount = 48,
                            isUserLiked = true,
                            dateAdded = "Aug 2026"
                        ),
                        GalleryItem(
                            title = "Sapphire Blue Crowntail Betta",
                            category = "Exotic Fish",
                            description = "Grade-A exhibition showcase halfmoon crown-tail Betta with deep metallic blue sheen and pristine finnage.",
                            imageUrl = "img_betta_fish",
                            tankSpecs = "30L Nano Cube • Sponge Filter • Live Java Fern • Almond Leaf Botanical Water",
                            floraFauna = "Crowntail Betta Splendens, Hornwort, Anubias Nana Petite",
                            likesCount = 94,
                            isUserLiked = true,
                            dateAdded = "Aug 2026"
                        ),
                        GalleryItem(
                            title = "Iwagumi Mountain Hardscape",
                            category = "Hardscapes",
                            description = "Zen-inspired minimalist Japanese Iwagumi layout composed with weathered Seiryu stones and crystal micro-carpeting flora.",
                            imageUrl = "img_aquascape_setup",
                            tankSpecs = "60x30x36cm Ultra-Clear 64L • Oase Biomaster 250 • Chihiros WRGB II Slim",
                            floraFauna = "Microrasbora Galaxy (Celestial Pearl Danio), Glossostigma Elatinoides",
                            likesCount = 67,
                            isUserLiked = false,
                            dateAdded = "Aug 2026"
                        ),
                        GalleryItem(
                            title = "Neon Tetra Paradise Aquasphere",
                            category = "Freshwater",
                            description = "Community nature tank packed with luxuriant stems, weeping moss on bogwood, and crystal healthy schooling fish.",
                            imageUrl = "img_hero_aquarium",
                            tankSpecs = "80cm Custom Aquarium 160L • Eheim Classic 350 • Fluval Plant 3.0",
                            floraFauna = "Green Neon Tetras, Harlequin Rasboras, Java Moss, Cryptocoryne Wendtii",
                            likesCount = 52,
                            isUserLiked = false,
                            dateAdded = "Aug 2026"
                        ),
                        GalleryItem(
                            title = "Vibrant Reef & Coral Sanctuary",
                            category = "Saltwater",
                            description = "Stunning marine ecosystem with fluorescent Euphyllia coral polyps, live clownfish pair, and pristine reef lighting spectrum.",
                            imageUrl = "img_hero_aquarium",
                            tankSpecs = "90L Saltwater Nano Reef • AI Prime 16HD • Tunze Comline Skimmer • Auto-Top-Off",
                            floraFauna = "Ocellaris Clownfish, Peppermint Shrimp, Torch Corals, Zoanthids",
                            likesCount = 89,
                            isUserLiked = true,
                            dateAdded = "Aug 2026"
                        ),
                        GalleryItem(
                            title = "High-Tech CO2 & Filtration Hub",
                            category = "Accessories",
                            description = "Pro-grade aquarium filtration and automated pressurized CO2 distribution manifold setup for ultra-clear aquatic scaping.",
                            imageUrl = "img_aquascape_setup",
                            tankSpecs = "Stainless Steel Inflow/Outflow Pipes • Inline CO2 Atomizer • Dosing Pump Station",
                            floraFauna = "Professional Aquarium Equipment & Monitoring Tools",
                            likesCount = 41,
                            isUserLiked = false,
                            dateAdded = "Aug 2026"
                        )
                    )
                )
            }

            if (productDao.getCount() == 0) {
                productDao.insertAll(
                    listOf(
                        ProductItem(
                            name = "Neon Tetra (School of 6)",
                            scientificName = "Paracheirodon innesi",
                            category = "Fishes",
                            price = 14.99,
                            originalPrice = 18.99,
                            stockQuantity = 45,
                            description = "Peaceful, active, and schooling freshwater fish with glowing iridescent neon blue and vivid red stripes. Ideal for planted community tanks.",
                            careLevel = "Easy",
                            waterParameters = "Temp: 22-26°C • pH: 6.0-7.0 • GH: 4-8",
                            imageUrl = "img_hero_aquarium",
                            badge = "Bestseller"
                        ),
                        ProductItem(
                            name = "Halfmoon Royal Blue Betta",
                            scientificName = "Betta splendens",
                            category = "Fishes",
                            price = 24.50,
                            originalPrice = 29.00,
                            stockQuantity = 12,
                            description = "Exotic hand-picked show-grade Betta with a stunning 180-degree caudal spread and radiant metallic cobalt hue.",
                            careLevel = "Easy",
                            waterParameters = "Temp: 24-28°C • pH: 6.5-7.5 • Low Current",
                            imageUrl = "img_betta_fish",
                            badge = "Exotic"
                        ),
                        ProductItem(
                            name = "Anubias Nana Petite (Live Plant)",
                            scientificName = "Anubias barteri var. nana",
                            category = "Plants",
                            price = 9.99,
                            originalPrice = 12.00,
                            stockQuantity = 30,
                            description = "Compact, hardy aquatic plant with deep green leaves that thrives attached to driftwood or stones without demanding high light.",
                            careLevel = "Easy",
                            waterParameters = "Temp: 20-28°C • Low-Medium Light • No CO2 required",
                            imageUrl = "img_aquascape_setup",
                            badge = "Hardy"
                        ),
                        ProductItem(
                            name = "Fish Garden OptiWhite 60P Tank (64L)",
                            scientificName = "Ultra-Clear Glass Tank",
                            category = "Aquarium Tanks",
                            price = 119.99,
                            originalPrice = 139.99,
                            stockQuantity = 8,
                            description = "High-transparency 6mm 91% clarity Optiwhite rimless glass aquarium with seamless bevelled silicone bonding.",
                            careLevel = "Moderate",
                            waterParameters = "Dimensions: 60 x 30 x 36 cm • 64 Litres",
                            imageUrl = "img_aquascape_setup",
                            badge = "Premium"
                        ),
                        ProductItem(
                            name = "Hikari Micro Pellets - Color Enhance (80g)",
                            scientificName = "Specialty Fish Food",
                            category = "Food & Nutrition",
                            price = 8.49,
                            originalPrice = 10.99,
                            stockQuantity = 50,
                            description = "Daily premium micro-pellet diet formulated with natural carotenoids to maximize the vivid coloration of small tropical fish.",
                            careLevel = "Easy",
                            waterParameters = "Slow-sinking • High Protein • Enriched Spirulina",
                            imageUrl = "img_hero_aquarium",
                            badge = "Top Rated"
                        ),
                        ProductItem(
                            name = "AquaClear Canister Filtration Unit 250",
                            scientificName = "Silent Biological Filter",
                            category = "Filters & Gear",
                            price = 79.99,
                            originalPrice = 95.00,
                            stockQuantity = 15,
                            description = "Multi-stage external canister filter with built-in priming pump, ceramic media, and ultra-quiet energy-efficient motor.",
                            careLevel = "Moderate",
                            waterParameters = "Flow Rate: 700 L/h • Suitable for 50-150L",
                            imageUrl = "img_aquascape_setup",
                            badge = "Essential"
                        )
                    )
                )
            }

            if (orderDao.getOrderCount() == 0) {
                // Pre-seed sample live orders for admin demonstration
                orderDao.insertOrder(
                    OrderEntity(
                        orderNumber = "FG-8921",
                        customerPhone = "+1 (555) 234-8901",
                        customerName = "David Miller",
                        deliveryAddress = "742 Evergreen Terrace, Springfield",
                        itemsSummary = "Halfmoon Royal Blue Betta (x1), Hikari Micro Pellets (x1)",
                        itemsJson = "[{\"name\":\"Halfmoon Royal Blue Betta\",\"price\":24.50,\"quantity\":1},{\"name\":\"Hikari Micro Pellets\",\"price\":8.49,\"quantity\":1}]",
                        subtotal = 32.99,
                        packingFee = 2.50,
                        deliveryFee = 0.0,
                        totalAmount = 35.49,
                        paymentMethod = "Cash on Delivery",
                        orderStatus = "Pending",
                        adminNotes = "Customer requested live fish oxygen pack. Call 15 min prior to arrival.",
                        timestamp = System.currentTimeMillis() - 1000 * 60 * 18,
                        syncedToWebAdmin = true
                    )
                )
                orderDao.insertOrder(
                    OrderEntity(
                        orderNumber = "FG-8919",
                        customerPhone = "+1 (555) 876-5432",
                        customerName = "Sarah Jenkins",
                        deliveryAddress = "120 Ocean View Boulevard, Apt 4B",
                        itemsSummary = "Neon Tetra School x6 (x2), Anubias Nana Petite (x2)",
                        itemsJson = "[{\"name\":\"Neon Tetra (School of 6)\",\"price\":14.99,\"quantity\":2},{\"name\":\"Anubias Nana Petite\",\"price\":9.99,\"quantity\":2}]",
                        subtotal = 49.96,
                        packingFee = 2.50,
                        deliveryFee = 0.0,
                        totalAmount = 52.46,
                        paymentMethod = "Online UPI / Card",
                        orderStatus = "Confirmed",
                        adminNotes = "Acclimation guide included in box.",
                        timestamp = System.currentTimeMillis() - 1000 * 60 * 85,
                        syncedToWebAdmin = true
                    )
                )
            }
        }
    }
}
