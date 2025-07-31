package com.xeta.arplacement.data

data class Drill(
    val id: Int,
    val name: String,
    val description: String,
    val tips: List<String>,
    val imageRes: Int = android.R.drawable.ic_menu_camera // Using system drawable as placeholder
)

object DrillRepository {
    val drills = listOf(
        Drill(
            id = 1,
            name = "Power Drill Pro",
            description = "High-performance cordless drill with variable speed control and LED work light. Perfect for heavy-duty drilling tasks in wood, metal, and masonry.",
            tips = listOf(
                "Use pilot holes for better accuracy",
                "Select appropriate drill bit for material",
                "Keep drill perpendicular to surface",
                "Apply steady pressure without forcing"
            )
        ),
        Drill(
            id = 2,
            name = "Impact Drill Max",
            description = "Heavy-duty impact drill designed for concrete and masonry work. Features hammer action for efficient drilling in tough materials.",
            tips = listOf(
                "Use hammer mode for concrete",
                "Wear safety glasses and dust mask",
                "Start with light pressure then increase",
                "Clear debris frequently from hole"
            )
        ),
        Drill(
            id = 3,
            name = "Precision Mini Drill",
            description = "Compact precision drill ideal for detailed work and small holes. Perfect for electronics, jewelry, and delicate materials.",
            tips = listOf(
                "Use low speed for better control",
                "Secure small pieces with clamps",
                "Keep drill bits sharp and clean",
                "Practice on scrap material first"
            )
        )
    )
}