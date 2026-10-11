/**
 * wr2a03_train_num.cpp — 若益宛 WR2-A03 车号系统 (原生 C++ 版)
 *
 * 源脚本 (JS 全量移植, 逐函数对应):
 *   train_num.js      → create / render / dispose, 侧线名变化才重绘,
 *                       透明清空 (AlphaComposite.CLEAR), 车厢数不符的错误提示,
 *                       每节车厢双侧牌 + 车头/车尾牌 + 每帧 drawCarModel
 *   draw_num.js       → drawNum (黑色居中, 基线 NUM_H - 12)
 *                       drawError (红色双语提示, SOURCE_HAN_SANS / 60px)
 *   train_num_util.js → parseSidingName (见 wr2a03_common.hpp)
 *
 * 四块几何 (与 JS 完全一致):
 *   num_left / num_right        1120×240 每节车厢两侧
 *   vehicle_num_forwards        1120×240 头牌 (第 1 节)
 *   vehicle_num_backwards       1120×240 尾牌 (最后一节)
 *
 * 纹理内容只在侧线名变化时重画 (JS 已有此优化), 其余每帧仅 upload +
 * drawCarModel —— 与 JS 行为逐帧等价。
 *
 * mtr_custom_resources.json:
 *   "vehicleScripts": [{
 *       "id": "wr2a03:train_num", "language": "cpp",
 *       "nativeLibrary": { "windows": "mtr:wr2a03/natives/windows-x64/wr2a03_train_num.dll",
 *                          "linux":   "mtr:wr2a03/natives/linux-x64/libwr2a03_train_num.so" }
 *   }]
 */
#include "wr2a03_common.hpp"

namespace wr2a03 {
namespace {

constexpr double NUM_FONT_SIZE_CAR = 60;    /* drawNum(g, num, 60) */
constexpr double NUM_FONT_SIZE_HEAD = 30;   /* drawNum(g, num, 30) */
const char* const NUM_ERROR_TEXT = "请检查侧线名。|Please check the siding name.";

/* Ordered rectangle corners, tilted by -10/+10 degrees around the centre. */
constexpr double PLATE_Y_TOP = 0.3125 + 0.3125 * 0.984807753012208;
constexpr double PLATE_Y_BOTTOM = 0.3125 - 0.3125 * 0.984807753012208;
constexpr double PLATE_Z_TILT = 0.3125 * 0.173648177666930;
constexpr double FWD_POS[4][3] = {
    {-1.4375, PLATE_Y_TOP,    -10.4875 - PLATE_Z_TILT},
    {-1.4375, PLATE_Y_BOTTOM, -10.4875 + PLATE_Z_TILT},
    { 1.4375, PLATE_Y_BOTTOM, -10.4875 + PLATE_Z_TILT},
    { 1.4375, PLATE_Y_TOP,    -10.4875 - PLATE_Z_TILT}
};
constexpr double BWD_POS[4][3] = {
    { 1.4375, PLATE_Y_TOP,    10.4875 + PLATE_Z_TILT},
    { 1.4375, PLATE_Y_BOTTOM, 10.4875 - PLATE_Z_TILT},
    {-1.4375, PLATE_Y_BOTTOM, 10.4875 - PLATE_Z_TILT},
    {-1.4375, PLATE_Y_TOP,    10.4875 + PLATE_Z_TILT}
};

/* ==================================================================== */
/* draw_num.js                                                           */
/* ==================================================================== */

/* clearTextureTransparent — AlphaComposite.CLEAR 等价物:
   整块纹理写成全透明 (0x00000000)，而不是白底。 */
void clear_texture_transparent(GraphicsTexture& tex) {
    tex.fill(CLEAR_COLOR);
}

/* drawNum — 以 (NUM_W - width) / 2 居中, 基线 NUM_H - 12 */
void draw_num(Gfx2D& g, const std::string& num, double fontSize) {
    if (num.empty()) return;
    const double w = g.font_width(fontSize, num.c_str(), FONT_SANS_LAO);
    const double x = (NUM_W - w) / 2.0;
    const double y = NUM_H - 12;
    g.set_color(BLACK_COLOR);
    g.draw_text(x, y, fontSize, num.c_str(), FONT_SANS_LAO);
}

/* drawError — 车厢数与车厢号数量不符时的红色双语提示 */
void draw_error(Gfx2D& g) {
    const double w = g.font_width(60, NUM_ERROR_TEXT, FONT_HAN_REGULAR);
    const double x = (NUM_W - w) / 2.0;
    const double y = NUM_H - 12;
    g.set_color(RED_COLOR);
    g.draw_text(x, y, 60, NUM_ERROR_TEXT, FONT_HAN_REGULAR);
}

/* ==================================================================== */
/* train_num.js — 状态与生命周期                                          */
/* ==================================================================== */

struct CarNumScreens {
    GraphicsTexture left;
    GraphicsTexture right;
    std::array<int32_t, 2> modelsLeft{{-1, -1}};
    std::array<int32_t, 2> modelsRight{{-1, -1}};
};

struct Wr2TrainNumState {
    std::vector<CarNumScreens> cars;
    GraphicsTexture fwd;
    GraphicsTexture bwd;
    int32_t modelFwd = -1;
    int32_t modelBwd = -1;
    std::string sidingNumBefore;    /* JS: state.sidingNumBefore */
    bool fwdDirty = true;           /* 需要重画标志（原生侧 bookkeeping） */
    bool bwdDirty = true;
};

struct Wr2TrainNumScript : VehicleScript<Wr2TrainNumState> {
    static constexpr auto ID = "wr2a03:train_num";

    void create(VehicleContext& ctx, Wr2TrainNumState& state, const Train& train) override {
        build_cars(ctx.input(), state, train);

        /* JS create(): 全部纹理先清成透明并上传一次 */
        for (CarNumScreens& cs : state.cars) {
            clear_texture_transparent(cs.left);
            clear_texture_transparent(cs.right);
            cs.left.upload(ctx.frame());
            cs.right.upload(ctx.frame());
        }
        if (state.fwd.width() == 0) state.fwd.create(ctx.input(), NUM_W, NUM_H);
        if (state.bwd.width() == 0) state.bwd.create(ctx.input(), NUM_W, NUM_H);
        clear_texture_transparent(state.fwd);
        state.fwd.upload(ctx.frame());
        clear_texture_transparent(state.bwd);
        state.bwd.upload(ctx.frame());

        build_models(ctx.input(), state);
        state.sidingNumBefore.clear();
        state.fwdDirty = state.bwdDirty = true;
    }

    void render(VehicleContext& ctx, Wr2TrainNumState& state, const Train& train) override {
        const int carCount = train.car_count();

        /* 车厢数变化 → 重建 DisplayHelper（JS 同款守卫） */
        if (static_cast<int>(state.cars.size()) != carCount) {
            build_cars(ctx.input(), state, train);
            build_models(ctx.input(), state);
            state.sidingNumBefore.clear();
            state.fwdDirty = state.bwdDirty = true;
        }

        /* 侧线名（车号来源） */
        const std::string sidingNameNow(train.siding_name().str());

        /* 只在侧线名变化时更新纹理内容（JS 同款短路） */
        if (!sidingNameNow.empty() && state.sidingNumBefore != sidingNameNow) {
            state.sidingNumBefore = sidingNameNow;

            const SidingInfo parsed = parse_siding_name(sidingNameNow);
            const std::string& vehicleNum = parsed.vehicleNum;
            const std::vector<std::string>& carNum = parsed.carNums;
            const bool mismatch = (carCount != static_cast<int>(carNum.size()));

            for (int i = 0; i < carCount && i < static_cast<int>(state.cars.size()); i++) {
                CarNumScreens& cs = state.cars[static_cast<size_t>(i)];
                clear_texture_transparent(cs.left);
                clear_texture_transparent(cs.right);

                {
                    Gfx2D gL(cs.left, ctx.host(), FONT_HAN_SANS, FONT_ARIAL);
                    if (mismatch) draw_error(gL);
                    else          draw_num(gL, carNum[static_cast<size_t>(i)], NUM_FONT_SIZE_CAR);
                }
                {
                    Gfx2D gR(cs.right, ctx.host(), FONT_HAN_SANS, FONT_ARIAL);
                    if (mismatch) draw_error(gR);
                    else          draw_num(gR, carNum[static_cast<size_t>(i)], NUM_FONT_SIZE_CAR);
                }
            }

            /* 车头牌 */
            {
                Gfx2D gF(state.fwd, ctx.host(), FONT_HAN_SANS, FONT_ARIAL);
                clear_texture_transparent(state.fwd);
                draw_num(gF, vehicleNum, NUM_FONT_SIZE_HEAD);
            }
            /* 车尾牌 */
            {
                Gfx2D gB(state.bwd, ctx.host(), FONT_HAN_SANS, FONT_ARIAL);
                clear_texture_transparent(state.bwd);
                draw_num(gB, vehicleNum, NUM_FONT_SIZE_HEAD);
            }
        }

        /* 每帧提交贴图 + 挂模型（JS: dh.upload() + ctx.drawCarModel） */
        for (int i = 0; i < carCount && i < static_cast<int>(state.cars.size()); i++) {
            CarNumScreens& cs = state.cars[static_cast<size_t>(i)];
            cs.left.upload(ctx.frame());
            cs.right.upload(ctx.frame());
            for (int32_t model : cs.modelsLeft) ctx.draw_car_model(model, i, nullptr);
            for (int32_t model : cs.modelsRight) ctx.draw_car_model(model, i, nullptr);
        }

        state.fwd.upload(ctx.frame());
        state.bwd.upload(ctx.frame());
        if (carCount > 0) {
            ctx.draw_car_model(state.modelFwd, 0, nullptr);
            ctx.draw_car_model(state.modelBwd, carCount - 1, nullptr);
        }
    }

    void dispose(VehicleContext& ctx, Wr2TrainNumState& state, const Train&) override {
        release_quad(ctx.host(), state.modelFwd);
        release_quad(ctx.host(), state.modelBwd);
        state.modelFwd = state.modelBwd = -1;

        for (CarNumScreens& cs : state.cars) {
            for (int32_t model : cs.modelsLeft) release_quad(ctx.host(), model);
            for (int32_t model : cs.modelsRight) release_quad(ctx.host(), model);
            cs.left.close(&ctx.input());
            cs.right.close(&ctx.input());
        }
        state.cars.clear();
        state.fwd.close(&ctx.input());
        state.bwd.close(&ctx.input());
    }

private:
    void build_cars(const JcmFrameInput& in, Wr2TrainNumState& state, const Train& train) {
        for (CarNumScreens& cs : state.cars) {
            for (int32_t model : cs.modelsLeft) release_quad(in.host, model);
            for (int32_t model : cs.modelsRight) release_quad(in.host, model);
            cs.left.close(&in);
            cs.right.close(&in);
        }
        state.cars.clear();
        const int carCount = train.car_count();
        for (int i = 0; i < carCount; i++) {
            CarNumScreens cs;
            cs.left.create(in, NUM_W, NUM_H);
            cs.right.create(in, NUM_W, NUM_H);
            for (int j = 0; j < 2; j++) {
                const double offset = j == 0 ? -7.5 : 7.5;
                cs.modelsLeft[j] = acquire_quad(in.host, cs.left.handle(), NUM_LEFT_POS,
                                                0.f, 0.f, 1.f, 1.f, 1, offset);
                cs.modelsRight[j] = acquire_quad(in.host, cs.right.handle(), NUM_RIGHT_POS,
                                                 0.f, 0.f, 1.f, 1.f, 1, offset);
            }
            state.cars.push_back(std::move(cs));
        }
    }

    void build_models(const JcmFrameInput& in, Wr2TrainNumState& state) {
        release_quad(in.host, state.modelFwd);
        release_quad(in.host, state.modelBwd);
        state.modelFwd = state.modelBwd = -1;

        if (state.fwd.width() > 0) {
            state.modelFwd = acquire_quad(in.host, state.fwd.handle(),
                                          FWD_POS, 0.f, 0.f, 1.f, 1.f);
        }
        if (state.bwd.width() > 0) {
            state.modelBwd = acquire_quad(in.host, state.bwd.handle(),
                                          BWD_POS, 0.f, 0.f, 1.f, 1.f);
        }
    }
};

MTR_REGISTER_VEHICLE_SCRIPT(Wr2TrainNumScript)

} /* anonymous namespace */
} /* namespace wr2a03 */
