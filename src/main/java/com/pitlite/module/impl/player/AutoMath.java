package com.pitlite.module.impl.player;

import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Stack;

public class AutoMath extends Module {
    private static final String QUICK_MATHS_PREFIX = "QUICK MATHS! Solve: ";

    private final BooleanSetting screenAnswer = new BooleanSetting("Screen Answer", true);
    private final NumberSetting screenDuration = new NumberSetting("Screen Duration", 3500, 1000, 8000, 100);

    private String screenText;
    private long screenShownAtMs;
    private long screenUntilMs;

    public AutoMath() {
        super("AutoMath", "Solves Quick Maths and prints the answer. Never auto-submits.", Category.PLAYER);
        addSettings(screenAnswer, screenDuration);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isToggled() || mc.thePlayer == null) {
            return;
        }

        String message = event.message.getUnformattedText();
        if (!message.startsWith(QUICK_MATHS_PREFIX)) {
            return;
        }

        String problem = message.substring(QUICK_MATHS_PREFIX.length());
        String equation = problem.replace("x", "*");

        try {
            double result = solveEquation(equation);
            long roundedResult = Math.round(result);
            String answerText = Long.toString(roundedResult);

            String chatLine = EnumChatFormatting.LIGHT_PURPLE + "" + EnumChatFormatting.BOLD + "Quick Maths "
                    + EnumChatFormatting.GRAY + problem.trim() + EnumChatFormatting.DARK_GRAY + " -> "
                    + EnumChatFormatting.YELLOW + EnumChatFormatting.BOLD + answerText
                    + EnumChatFormatting.GRAY + " (type /ac " + answerText + ")";
            mc.thePlayer.addChatMessage(new ChatComponentText(chatLine));

            if (screenAnswer.enabled) {
                showScreenAnswer(answerText);
            }
        } catch (Exception ex) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "AutoMath: could not solve \"" + problem.trim() + "\" — " + ex.getMessage()));
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled() || screenText == null || mc.thePlayer == null) {
            return;
        }
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= screenUntilMs) {
            screenText = null;
            return;
        }

        long age = now - screenShownAtMs;
        long remaining = screenUntilMs - now;
        float fade = 1f;
        if (age < 250L) {
            fade = age / 250f;
        } else if (remaining < 400L) {
            fade = remaining / 400f;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        String title = EnumChatFormatting.LIGHT_PURPLE + "" + EnumChatFormatting.BOLD + "QUICK MATHS";
        String answer = EnumChatFormatting.YELLOW + "" + EnumChatFormatting.BOLD + screenText;

        int titleW = mc.fontRendererObj.getStringWidth(title);
        int answerW = mc.fontRendererObj.getStringWidth(answer);
        int boxW = Math.max(titleW, answerW) + 24;
        int boxH = 36;
        int left = centerX - boxW / 2;
        int top = centerY - boxH / 2 - 8;

        int bgAlpha = (int) (0xCC * fade) << 24;
        int accentAlpha = (int) (0xFF * fade) << 24;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        com.pitlite.utils.RenderUtils.drawRect(left, top, left + boxW, top + boxH, bgAlpha | 0x141418);
        com.pitlite.utils.RenderUtils.drawRect(left, top, left + 3, top + boxH, accentAlpha | 0xD78BFF);

        int textAlpha = (int) (255 * fade) << 24;
        mc.fontRendererObj.drawString(title, centerX - titleW / 2, top + 8, textAlpha | 0xFFFFFF);
        mc.fontRendererObj.drawString(answer, centerX - answerW / 2, top + 20, textAlpha | 0xFFFFFF);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void showScreenAnswer(String answer) {
        screenText = answer;
        screenShownAtMs = System.currentTimeMillis();
        screenUntilMs = screenShownAtMs + (long) screenDuration.value;
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("random.orb"), 1.0F));
    }

    @Override
    protected void onDisable() {
        screenText = null;
        screenShownAtMs = 0L;
        screenUntilMs = 0L;
    }

    private double solveEquation(String equation) throws Exception {
        equation = equation.replaceAll("\\s+", "");
        return evaluate(equation);
    }

    private double evaluate(String equation) throws Exception {
        Stack<Double> values = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (int i = 0; i < equation.length(); ++i) {
            char ch = equation.charAt(i);

            if (Character.isDigit(ch) || ch == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < equation.length() && (Character.isDigit(equation.charAt(i)) || equation.charAt(i) == '.')) {
                    sb.append(equation.charAt(i++));
                }
                values.push(Double.parseDouble(sb.toString()));
                --i;
                continue;
            }

            if (ch == '(') {
                operators.push(ch);
                continue;
            }

            if (ch == ')') {
                while (operators.peek() != '(') {
                    values.push(applyOperation(operators.pop(), values.pop(), values.pop()));
                }
                operators.pop();
                continue;
            }

            if (isOperator(ch)) {
                while (!operators.isEmpty() && precedence(ch) <= precedence(operators.peek())) {
                    values.push(applyOperation(operators.pop(), values.pop(), values.pop()));
                }
                operators.push(ch);
            }
        }

        while (!operators.isEmpty()) {
            values.push(applyOperation(operators.pop(), values.pop(), values.pop()));
        }

        return values.pop();
    }

    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/';
    }

    private int precedence(char operator) {
        switch (operator) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
            default:
                return -1;
        }
    }

    private double applyOperation(char operator, double b, double a) throws Exception {
        switch (operator) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                if (b == 0.0) {
                    throw new Exception("Cannot divide by zero");
                }
                return a / b;
            default:
                throw new Exception("Unsupported operator: " + operator);
        }
    }
}
