package com.pitlite.module.impl.render;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.utils.HudPositionManager;
import com.pitlite.utils.HudStackManager;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.render.RenderUtil;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module implements DraggableHud {
    private static final double DEFAULT_X = 50.0;
    private static final double DEFAULT_Y = 50.0;

    private final NumberSetting scaleSetting = new NumberSetting("Scale", 1.0, 0.5, 2.0, 1);
    private final NumberSetting backgroundOpacity = new NumberSetting("Background", 40.0, 0.0, 100.0, 0);
    private final BooleanSetting showHead = new BooleanSetting("Show Head", true);
    private final BooleanSetting showHealthText = new BooleanSetting("Health Text", true);
    private final BooleanSetting showIndicator = new BooleanSetting("W/D/L Indicator", true);
    private final BooleanSetting animations = new BooleanSetting("Animations", true);
    private final BooleanSetting textShadow = new BooleanSetting("Text Shadow", true);
    private final BooleanSetting showHearts = new BooleanSetting("Show Hearts", true);

    private EntityLivingBase target = null;
    private EntityLivingBase renderTarget = null;
    private ResourceLocation headTexture = null;
    private long lastHitTime = 0L;
    private static final long TARGET_EXPIRATION = 3000L;

    private static final float HUD_WIDTH = 150.0f;
    private static final float HUD_HEIGHT = 46.0f;

    private static class Spring {
        float value, velocity;
        Spring(float v) { this.value = v; }
        void update(float target, float stiffness, float damping) {
            float dt = 0.0166f;
            float force = stiffness * (target - value) - damping * velocity;
            velocity += force * dt;
            value += velocity * dt;
        }
    }

    private final Spring scaleSpring = new Spring(0f);
    private final Spring alphaSpring = new Spring(0f);
    private final Spring healthSpring = new Spring(20f);
    private final Spring damageLagSpring = new Spring(20f);

    private static final DecimalFormat healthFmt = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));

    public TargetHUD() {
        super("TargetHUD", "Displays information about your target", Category.RENDER);
        addSettings(scaleSetting, backgroundOpacity, showHead, showHealthText, showIndicator, animations, textShadow, showHearts);
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        updateTarget();
        if (target != null) renderTarget = target;

        boolean validTarget = target != null && !target.isDead && target.getHealth() > 0;
        boolean recentlyHit = System.currentTimeMillis() - lastHitTime < TARGET_EXPIRATION;

        float targetScale = (validTarget && recentlyHit) ? 1.0f : 0.8f;
        float targetAlpha = (validTarget && recentlyHit) ? 1.0f : 0.0f;
        if (!animations.enabled) {
            scaleSpring.value = targetScale;
            alphaSpring.value = targetAlpha;
        } else {
            scaleSpring.update(targetScale, 180f, 6f);
            alphaSpring.update(targetAlpha, 150f, 15f);
        }

        if (alphaSpring.value < 0.01f) { renderTarget = null; return; }

        if (renderTarget != null) {
            float currentHealth = renderTarget.getHealth() + renderTarget.getAbsorptionAmount();
            if (animations.enabled) {
                healthSpring.update(currentHealth, 180f, 15f);
                damageLagSpring.update(currentHealth, 60f, 12f);
            } else {
                healthSpring.value = currentHealth;
                damageLagSpring.value = currentHealth;
            }
        }
        doRender();
    }

    private void doRender() {
        float scale = (float) scaleSetting.value;
        float posX = getRenderX();
        float posY = getRenderY();
        float animScale = Math.max(0f, scaleSpring.value);
        float animAlpha = Math.max(0f, Math.min(1f, alphaSpring.value));

        if (renderTarget != null) {
            ResourceLocation skin = getSkin(renderTarget);
            if (skin != null) headTexture = skin;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(posX / scale, posY / scale, 0.0f);

        float centerX = HUD_WIDTH / 2f;
        float centerY = HUD_HEIGHT / 2f;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(animScale, animScale, 1f);
        GlStateManager.translate(-centerX, -centerY, 0);

        int bgOpacity = Math.max(15, (int) backgroundOpacity.value);
        int bgAlpha = (int) ((bgOpacity / 100.0f) * 255 * animAlpha);
        int backgroundColor = (bgAlpha << 24) | 0x1A1A1A;
        int shadowAlpha = (int) (50 * animAlpha);
        int shadowColor = (shadowAlpha << 24);

        RenderUtil.drawRoundedRect(2, 3, HUD_WIDTH, HUD_HEIGHT, 8f, shadowColor);
        RenderUtil.drawRoundedRect(0, 0, HUD_WIDTH, HUD_HEIGHT, 8f, backgroundColor);

        float faceSize = 30f;
        float faceX = 8f, faceY = 8f;
        if (!showHead.enabled) {
            faceSize = 0f;
            faceX = 4f;
        } else if (headTexture != null) {
            GL11.glColor4f(1f, 1f, 1f, animAlpha);
            mc.getTextureManager().bindTexture(headTexture);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Gui.drawScaledCustomSizeModalRect((int) faceX, (int) faceY, 8, 8, 8, 8, (int) faceSize, (int) faceSize, 64, 64);
            Gui.drawScaledCustomSizeModalRect((int) faceX, (int) faceY, 40, 8, 8, 8, (int) faceSize, (int) faceSize, 64, 64);
        } else {
            int placeholderBg = ((int) (255 * animAlpha) << 24) | 0x2E2E2E;
            RenderUtil.drawRoundedRect(faceX, faceY, faceSize, faceSize, 4f, placeholderBg);
        }

        float textX = showHead.enabled ? faceX + faceSize + 8f : faceX;
        String name = renderTarget != null ? renderTarget.getName() : "Unknown";
        int nameColor = ((int) (255 * animAlpha) << 24) | 0xFFFFFF;

        if (textShadow.enabled) {
            mc.fontRendererObj.drawStringWithShadow(name, textX, 10f, nameColor);
        } else {
            mc.fontRendererObj.drawString(name, (int) textX, 10, nameColor);
        }

        if (showIndicator.enabled && renderTarget != null) {
            float myHp = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
            float theirHp = renderTarget.getHealth() + renderTarget.getAbsorptionAmount();
            String status;
            int statusColor;
            if (Math.abs(theirHp - myHp) < 1.0f) { status = "D"; statusColor = 0xFFFFC83C; }
            else if (theirHp < myHp) { status = "W"; statusColor = 0xFF55FF55; }
            else { status = "L"; statusColor = 0xFFFF5555; }
            float indX = HUD_WIDTH - mc.fontRendererObj.getStringWidth(status) - 8f;
            int finalColor = (statusColor & 0x00FFFFFF) | ((int) (255 * animAlpha) << 24);
            if (textShadow.enabled) mc.fontRendererObj.drawStringWithShadow(status, indX, 11f, finalColor);
            else mc.fontRendererObj.drawString(status, (int) indX, 11, finalColor);
        }

        float maxH = renderTarget != null ? renderTarget.getMaxHealth() + renderTarget.getAbsorptionAmount() : 20f;
        if (maxH == 0) maxH = 20f;

        if (showHealthText.enabled) {
            float displayHp = showHearts.enabled ? healthSpring.value / 2.0f : healthSpring.value;
            String hpText = healthFmt.format(displayHp) + " \u2764";
            float hpX = showIndicator.enabled ? HUD_WIDTH - mc.fontRendererObj.getStringWidth(hpText) - 25f
                    : HUD_WIDTH - mc.fontRendererObj.getStringWidth(hpText) - 8f;
            int hpColor = ((int) (255 * animAlpha) << 24) | 0xAAAAAA;
            if (textShadow.enabled) mc.fontRendererObj.drawStringWithShadow(hpText, hpX, 11f, hpColor);
            else mc.fontRendererObj.drawString(hpText, (int) hpX, 11, hpColor);
        }

        float barX = textX, barY = 28f, barW = HUD_WIDTH - textX - 8f, barH = 5f;
        int trackColor = ((int) (60 * animAlpha) << 24) | 0x333333;
        RenderUtil.drawRoundedRect(barX, barY, barW, barH, barH / 2f, trackColor);

        float hpPct = Math.max(0f, Math.min(1f, healthSpring.value / maxH));
        float lagPct = Math.max(0f, Math.min(1f, damageLagSpring.value / maxH));
        float hpFill = barW * hpPct;
        float lagFill = barW * lagPct;

        int healthColor;
        if (hpPct > 0.5f) healthColor = interpolateColor(0xFFFFB300, 0xFF00E676, (hpPct - 0.5f) * 2f);
        else healthColor = interpolateColor(0xFFFF1744, 0xFFFFB300, hpPct * 2f);
        healthColor = (healthColor & 0x00FFFFFF) | ((int) (255 * animAlpha) << 24);
        int lagColor = ((int) (180 * animAlpha) << 24) | 0xFF1744;

        if (lagFill > hpFill) RenderUtil.drawRoundedRect(barX, barY, lagFill, barH, barH / 2f, lagColor);
        RenderUtil.drawRoundedRect(barX, barY, Math.max(barH, hpFill), barH, barH / 2f, healthColor);

        GlStateManager.popMatrix();
    }

    private int interpolateColor(int c1, int c2, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int a1=(c1>>24)&0xff, r1=(c1>>16)&0xff, g1=(c1>>8)&0xff, b1=c1&0xff;
        int a2=(c2>>24)&0xff, r2=(c2>>16)&0xff, g2=(c2>>8)&0xff, b2=c2&0xff;
        return ((int)(a1+(a2-a1)*f)<<24)|((int)(r1+(r2-r1)*f)<<16)|((int)(g1+(g2-g1)*f)<<8)|(int)(b1+(b2-b1)*f);
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!isToggled()) return;
        if (event.entityPlayer == mc.thePlayer && event.target instanceof EntityPlayer) {
            EntityPlayer attacked = (EntityPlayer) event.target;
            if (!isValidTarget(attacked)) return;
            if (attacked != target) {
                float total = attacked.getHealth() + attacked.getAbsorptionAmount();
                healthSpring.value = total;
                damageLagSpring.value = total;
                healthSpring.velocity = 0f;
                damageLagSpring.velocity = 0f;
            }
            target = attacked;
            lastHitTime = System.currentTimeMillis();
        }
    }

    private boolean isValidTarget(EntityPlayer p) {
        if (p.isInvisible() || mc.getNetHandler() == null) return false;
        if (mc.getNetHandler().getPlayerInfo(p.getUniqueID()) == null) return false;
        if (p.getUniqueID().version() == 2) return false;
        String name = p.getName();
        return !name.startsWith("[NPC]") && !name.contains("CITIZEN");
    }

    private void updateTarget() {
        if (target != null) {
            if (target.isDead || target.getHealth() <= 0 || mc.thePlayer.getDistanceToEntity(target) > 16.0
                    || System.currentTimeMillis() - lastHitTime > TARGET_EXPIRATION) {
                target = null;
            }
        }
    }

    private ResourceLocation getSkin(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(((EntityPlayer) entity).getName());
            if (info != null) return info.getLocationSkin();
        }
        return null;
    }

    @Override
    public String getHudKey() {
        return getName();
    }

    @Override
    public boolean isHudVisible() {
        return renderTarget != null;
    }

    @Override
    public int getHudX() {
        return (int) HudPositionManager.getX(getHudKey(), DEFAULT_X);
    }

    @Override
    public int getHudY() {
        return (int) HudPositionManager.getY(getHudKey(), DEFAULT_Y);
    }

    @Override
    public int getHudWidth() {
        return (int) (HUD_WIDTH * scaleSetting.value);
    }

    @Override
    public int getHudHeight() {
        return (int) (HUD_HEIGHT * scaleSetting.value);
    }
}
