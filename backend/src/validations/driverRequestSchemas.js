const { z } = require("zod");

const createDriverRequestSchema = z.object({
  body: z.object({
    idCard: z.string().trim().url("Đường dẫn ảnh CCCD không hợp lệ").max(500, "Đường dẫn ảnh CCCD quá dài"),
    driverLicense: z.string().trim().url("Đường dẫn ảnh GPLX không hợp lệ").max(500, "Đường dẫn ảnh GPLX quá dài"),
    vehicleRegistration: z.string().trim().url("Đường dẫn ảnh đăng ký xe không hợp lệ").max(500, "Đường dẫn ảnh đăng ký xe quá dài"),
    vehicleType: z.enum(["motorbike", "car", "Motor", "Taxi"], {
      errorMap: () => ({ message: "Loại xe phải là 'motorbike' hoặc 'car'" })
    })
  })
});

const updateDriverRequestStatusSchema = z.object({
  params: z.object({
    id: z.coerce.number().int().positive("Mã yêu cầu tài xế không hợp lệ")
  }),
  body: z.object({
    status: z.enum(["Approved", "Rejected"])
  })
});

module.exports = {
  createDriverRequestSchema,
  updateDriverRequestStatusSchema
};
