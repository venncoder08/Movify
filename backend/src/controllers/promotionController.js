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

function getPromotionName(body) {
  return body.name ?? body.code ?? body.title;
}

function getPromotionDiscountValue(body) {
  return body.discountValue ?? body.value ?? body.discount ?? body.amount;
}

async function listPromotions(req, res) {
  try {
    const promotions = await prisma.promotion.findMany({
      where: {
        DeletedAt: null
      },
      orderBy: {
        CreatedAt: "desc"
      },
      select: {
        ID: true,
        Name: true,
        Type: true,
        DiscountValue: true
      }
    });

    const promotionData = promotions.map((promotion) => {
      const discountValue = Number(promotion.DiscountValue);

      return {
        id: promotion.ID,
        code: promotion.Name,
        title: promotion.Name,
        type: promotion.Type,
        discountValue,
        description: buildPromotionDescription(promotion.Type, discountValue)
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

async function createPromotion(req, res) {
  try {
    const { type } = req.body;
    const name = getPromotionName(req.body);
    const discountValue = getPromotionDiscountValue(req.body);

    if (!name || !type || discountValue === undefined) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Tên khuyến mãi, loại và giá trị giảm là bắt buộc"
      });
    }

    const normalizedType = String(type || "").toLowerCase().trim();
    if (!["percent", "vnd"].includes(normalizedType)) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Loại khuyến mãi phải là 'percent' hoặc 'vnd'"
      });
    }

    const numericValue = Number(discountValue);
    if (isNaN(numericValue) || numericValue <= 0) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Giá trị giảm phải là số dương"
      });
    }

    const existingPromotion = await prisma.promotion.findFirst({
      where: {
        Name: name.trim(),
        DeletedAt: null
      }
    });

    if (existingPromotion) {
      return res.status(409).json({
        error: "Conflict",
        message: "Khuyến mãi này đã tồn tại"
      });
    }

    const createdPromotion = await prisma.promotion.create({
      data: {
        Name: name.trim(),
        Type: normalizedType,
        DiscountValue: numericValue,
        AdminID: req.user.id
      }
    });

    res.status(201).json({
      message: "Tạo khuyến mãi thành công",
      data: {
        id: createdPromotion.ID,
        code: createdPromotion.Name,
        title: createdPromotion.Name,
        type: createdPromotion.Type,
        discountValue: Number(createdPromotion.DiscountValue),
        description: buildPromotionDescription(createdPromotion.Type, createdPromotion.DiscountValue)
      }
    });
  } catch (error) {
    console.error("Create promotion error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function updatePromotion(req, res) {
  try {
    const promotionId = Number(req.params.id);
    const { type } = req.body;
    const name = getPromotionName(req.body);
    const discountValue = getPromotionDiscountValue(req.body);

    if (!Number.isInteger(promotionId) || promotionId <= 0) {
      return res.status(400).json({
        error: "Validation Error",
        message: "ID khuyến mãi không hợp lệ"
      });
    }

    const promotion = await prisma.promotion.findUnique({
      where: { ID: promotionId }
    });

    if (!promotion || promotion.DeletedAt) {
      return res.status(404).json({
        error: "Not Found",
        message: "Không tìm thấy khuyến mãi"
      });
    }

    const updateData = {};

    if (name !== undefined) {
      const trimmedName = String(name).trim();
      if (!trimmedName) {
        return res.status(400).json({
          error: "Validation Error",
          message: "Tên khuyến mãi không được để trống"
        });
      }

      const existingPromotion = await prisma.promotion.findFirst({
        where: {
          Name: trimmedName,
          DeletedAt: null,
          NOT: { ID: promotionId }
        }
      });

      if (existingPromotion) {
        return res.status(409).json({
          error: "Conflict",
          message: "Khuyến mãi với tên này đã tồn tại"
        });
      }

      updateData.Name = trimmedName;
    }

    if (type !== undefined) {
      const normalizedType = String(type).toLowerCase().trim();
      if (!["percent", "vnd"].includes(normalizedType)) {
        return res.status(400).json({
          error: "Validation Error",
          message: "Loại khuyến mãi phải là 'percent' hoặc 'vnd'"
        });
      }
      updateData.Type = normalizedType;
    }

    if (discountValue !== undefined) {
      const numericValue = Number(discountValue);
      if (isNaN(numericValue) || numericValue <= 0) {
        return res.status(400).json({
          error: "Validation Error",
          message: "Giá trị giảm phải là số dương"
        });
      }
      updateData.DiscountValue = numericValue;
    }

    if (Object.keys(updateData).length === 0) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Phải cung cấp ít nhất một trường để cập nhật"
      });
    }

    const updatedPromotion = await prisma.promotion.update({
      where: { ID: promotionId },
      data: updateData
    });

    res.status(200).json({
      message: "Cập nhật khuyến mãi thành công",
      data: {
        id: updatedPromotion.ID,
        code: updatedPromotion.Name,
        title: updatedPromotion.Name,
        type: updatedPromotion.Type,
        discountValue: Number(updatedPromotion.DiscountValue),
        description: buildPromotionDescription(updatedPromotion.Type, updatedPromotion.DiscountValue)
      }
    });
  } catch (error) {
    console.error("Update promotion error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function deletePromotion(req, res) {
  try {
    const promotionId = Number(req.params.id);

    if (!Number.isInteger(promotionId) || promotionId <= 0) {
      return res.status(400).json({
        error: "Validation Error",
        message: "ID khuyến mãi không hợp lệ"
      });
    }

    const promotion = await prisma.promotion.findUnique({
      where: { ID: promotionId }
    });

    if (!promotion || promotion.DeletedAt) {
      return res.status(404).json({
        error: "Not Found",
        message: "Không tìm thấy khuyến mãi"
      });
    }

    const deletedPromotion = await prisma.promotion.update({
      where: { ID: promotionId },
      data: {
        DeletedAt: new Date()
      }
    });

    res.status(200).json({
      message: "Xóa khuyến mãi thành công",
      data: {
        id: deletedPromotion.ID,
        code: deletedPromotion.Name,
        title: deletedPromotion.Name,
        type: deletedPromotion.Type,
        discountValue: Number(deletedPromotion.DiscountValue),
        description: buildPromotionDescription(deletedPromotion.Type, deletedPromotion.DiscountValue)
      }
    });
  } catch (error) {
    console.error("Delete promotion error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  listPromotions,
  createPromotion,
  updatePromotion,
  deletePromotion
};
