const prisma = require("../config/prisma");

function formatDiscountValue(value) {
  if (Number.isInteger(value)) {
    return String(value);
  }

  return value.toFixed(2).replace(/\.?0+$/, "");
}

function buildPromotionDescription(type, discountValue) {
  const normalizedType = String(type || "").toLowerCase();
  const formattedValue = formatDiscountValue(discountValue);

  if (normalizedType.includes("percent")) {
    return `Giảm ${formattedValue}% cho chuyến đi`;
  }

  return `Giảm ${formattedValue} VND cho chuyến đi`;
}

async function listPromotions(req, res) {
  try {
    const promotions = await prisma.promotion.findMany({
      where: {
        deletedAt: null
      },
      orderBy: {
        createdAt: "desc"
      },
      select: {
        id: true,
        name: true,
        type: true,
        discountValue: true
      }
    });

    const promotionData = promotions.map((promotion) => {
      const discountValue = Number(promotion.discountValue);

      return {
        id: promotion.id,
        code: promotion.name,
        title: promotion.name,
        type: promotion.type,
        discountValue,
        description: buildPromotionDescription(promotion.type, discountValue)
      };
    });

    res.status(200).json({
      data: promotionData
    });
  } catch (error) {
    console.error("List promotions error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  listPromotions
};
