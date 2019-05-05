package fuzs.tradinggui.gui;

import fuzs.tradinggui.gui.helper.TradingRecipe;
import fuzs.tradinggui.gui.helper.TradingRecipeList;
import fuzs.tradinggui.inventory.ContainerVillager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SideOnly(Side.CLIENT)
public class GuiTradingBook extends Gui
{
    private static final ResourceLocation RECIPE_BOOK = new ResourceLocation("textures/gui/container/merchant_book.png");
    private final int xSize = 112;
    private final int ySize = 166;
    private Minecraft mc;
    private GuiButtonTradingRecipe hoveredButton;
    public static final int BUTTON_SPACE = 6;
    private List<GuiButtonTradingRecipe> buttonList = new ArrayList<>(6);
    private GuiTextField searchField;
    private String lastSearch = "";
    private int guiLeft;
    private int guiTop;
    private boolean sentRecipeList;
    private boolean populate;
    private TradingRecipeList tradingRecipeList;
    /** Amount scrolled in Creative mode inventory (0 = top, 1 = bottom) */
    private float currentScroll;
    private int scrollPostion = 0;
    /** True if the scrollbar is being dragged */
    private boolean isScrolling;
    /** True if the left mouse button was held down last time drawScreen was called. */
    private boolean wasClicking;
    /** The button that was just pressed. */
    private GuiButton selectedButton;
    private int selectedTradingRecipe;
    private boolean clearSearch;
    public int hoveredSlot;
    private int timesInventoryChanged;

    public void initGui(Minecraft mc, int width, int height)
    {
        this.mc = mc;
        this.guiLeft = (width - xSize) / 2 - 88;
        this.guiTop = (height - ySize) / 2;
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        this.timesInventoryChanged = mc.player.inventory.getTimesChanged();
        this.searchField = new GuiTextField(0, mc.fontRenderer, this.guiLeft + 9, this.guiTop + 9,
                80, mc.fontRenderer.FONT_HEIGHT);
        this.searchField.setMaxStringLength(50);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setFocused(true);
        this.searchField.setCanLoseFocus(false);
        this.searchField.setTextColor(16777215);
        this.sentRecipeList = false;
        this.populate = false;
        this.selectedTradingRecipe = 0;
        this.clearSearch = false;

        for (int i = 0; i < BUTTON_SPACE; ++i)
        {
            this.buttonList.add(new GuiButtonTradingRecipe(i, this.guiLeft + 10, this.guiTop + 24 + 22 * i));
            this.buttonList.get(i).visible = false;
        }

    }

    public void removed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    public void setSelectedTradingRecipe(int i) {

        if (this.tradingRecipeList != null) {
            this.tradingRecipeList.get(this.selectedTradingRecipe).setSelected(false);
            this.selectedTradingRecipe = i;
            this.tradingRecipeList.get(this.selectedTradingRecipe).setSelected(true);
            this.populate = true;
        } else {
            this.selectedTradingRecipe = i;
        }

    }

    public void countContents(ContainerVillager container)
    {
        if (this.tradingRecipeList != null) {
            this.tradingRecipeList.countRecipeContents(container);
        }
    }

    public void update(MerchantRecipeList merchantrecipelist, ContainerVillager container)
    {
        if (!this.sentRecipeList) {
            this.tradingRecipeList = new TradingRecipeList(merchantrecipelist);
            this.tradingRecipeList.get(this.selectedTradingRecipe).setSelected(true);
            this.countContents(container);
            this.sentRecipeList = true;
            this.populate = true;
        }

        if (this.tradingRecipeList != null && this.populate) {

            if (this.tradingRecipeList.size() != merchantrecipelist.size()) {
                return;
            }

            int i = this.scrollPostion;

            for (GuiButtonTradingRecipe guiButtonTradingRecipe : this.buttonList) {

                guiButtonTradingRecipe.visible = false;

                for (int j = i; j < this.tradingRecipeList.size(); j++) {

                    TradingRecipe tradingRecipe = this.tradingRecipeList.get(j);

                    if (tradingRecipe.isValidRecipe() && tradingRecipe.getActive()) {
                        guiButtonTradingRecipe.setContents(j, tradingRecipe, merchantrecipelist.get(j).isRecipeDisabled());
                        i = j + 1;
                        guiButtonTradingRecipe.visible = true;
                        break;
                    } else {
                        i++;
                    }
                }

            }

            this.populate = false;

        }

        if (this.timesInventoryChanged != this.mc.player.inventory.getTimesChanged())
        {
            this.countContents(container);
            this.timesInventoryChanged = this.mc.player.inventory.getTimesChanged();
        }

        if (this.clearSearch) {
            this.searchField.setCursorPositionEnd();
            this.searchField.setSelectionPos(0);
        }
    }

    public void render(int mouseX, int mouseY, float partialTicks)
    {
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 100.0F);
        this.mc.getTextureManager().bindTexture(RECIPE_BOOK);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.searchField.drawTextBox();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.tradingRecipeList != null) {

            boolean flag = Mouse.isButtonDown(0);
            float h2 = 1.0F / (float) Math.sqrt((float) Math.max(this.tradingRecipeList.activeRecipeSize() - BUTTON_SPACE + 1, 1));
            int height = (int) (h2 * 74) * 2; //casting before doubling so it always lines up at the bottom with the added stripe
            int i = this.guiLeft + 98;
            int j = this.guiTop + 8;
            int k = i + 6;
            int l = j + 149;
            boolean scrollable = this.tradingRecipeList.scrollable();
            this.mc.getTextureManager().bindTexture(RECIPE_BOOK);
            this.drawTexturedModalRect(i, j + (int) ((float) (l - j - height) * this.currentScroll), scrollable ? 196 : 202, 0, 6, height);
            this.drawTexturedModalRect(i, j + height + (int) ((float) (l - j - height) * this.currentScroll), scrollable ? 196 : 202, 148, 6, 1);

            if (!this.wasClicking && flag && mouseX >= i && mouseY >= j && mouseX < k && mouseY < l + 1) {
                this.isScrolling = scrollable;
            }

            if (!flag) {
                this.isScrolling = false;
            }

            this.wasClicking = flag;

            if (this.isScrolling) {
                this.currentScroll = ((float) (mouseY - j) - 7.5F) / ((float) (l - j) - 15.0F);
                this.currentScroll = MathHelper.clamp(this.currentScroll, 0.0F, 1.0F);
                this.scrollTo(this.currentScroll);
            }

        }

        RenderHelper.disableStandardItemLighting();

        this.hoveredButton = null;

        for (GuiButtonTradingRecipe guiButtonTradingRecipe : this.buttonList) {

            guiButtonTradingRecipe.drawButton(this.mc, mouseX, mouseY, partialTicks);

            if (guiButtonTradingRecipe.isMouseOver() && guiButtonTradingRecipe.visible)
            {
                this.hoveredButton = guiButtonTradingRecipe;
            }

        }

        GlStateManager.popMatrix();
    }

    public void renderHoveredTooltip(int mouseX, int mouseY)
    {
        if (mc.currentScreen != null && this.hoveredButton != null)
        {
            List<String> tooltip = this.hoveredButton.getToolTipText(mc.currentScreen, mouseX, mouseY);
            if (tooltip != null && mc.player.inventory.getItemStack().isEmpty()) {
                mc.currentScreen.drawHoveringText(tooltip, mouseX, mouseY);
            }
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected int mouseClicked(int mouseX, int mouseY, int mouseButton) {

        if (this.searchField.mouseClicked(mouseX, mouseY, mouseButton)) {
            return -2;
        }

        if (mouseButton == 0 || mouseButton == 1) {
            for (GuiButtonTradingRecipe guiButtonTradingRecipe : this.buttonList) {

                if (guiButtonTradingRecipe.mousePressed(this.mc, mouseX, mouseY)) {
                    this.clearSearch = true;
                    this.selectedButton = guiButtonTradingRecipe;
                    guiButtonTradingRecipe.playPressSound(this.mc.getSoundHandler());
                    return guiButtonTradingRecipe.getRecipeId();
                }
            }
        }

        return -1;

    }

    /**
     * Called when a mouse button is released.
     */
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (this.selectedButton != null && state == 0)
        {
            this.selectedButton.mouseReleased(mouseX, mouseY);
            this.selectedButton = null;
        }
    }

    /**
     * Handles mouse input.
     */
    public void handleMouseInput()
    {
        int i = Mouse.getEventDWheel();

        if (i != 0 && this.tradingRecipeList != null && this.tradingRecipeList.scrollable())
        {
            int j = this.tradingRecipeList.activeRecipeSize();

            if (i > 0)
            {
                i = 1;
            }

            if (i < 0)
            {
                i = -1;
            }

            this.currentScroll = (float)((double)this.currentScroll - (double)i / (double)j);
            this.currentScroll = MathHelper.clamp(this.currentScroll, 0.0F, 1.0F);
            this.scrollTo(this.currentScroll);
        }
    }

    public boolean hasRecipeContents(int id) {
        if (this.tradingRecipeList != null && this.tradingRecipeList.size() > id) {
            return this.tradingRecipeList.get(id).hasRecipeContents();
        }
        return false;
    }

    public boolean hasClickedOutside(int mouseX, int mouseY, int guiLeft, int guiTop, int xSize, int ySize)
    {
        boolean flag = mouseX < guiLeft || mouseY < guiTop || mouseX >= guiLeft + xSize || mouseY >= guiTop + ySize;
        boolean flag1 = guiLeft - this.xSize < mouseX && mouseX < guiLeft && guiTop < mouseY && mouseY < guiTop + ySize;
        return flag && !flag1;
    }

    public boolean keyPressed(char typedChar, int keyCode)
    {
        if (this.checkValidKeys(keyCode)) {
            return false;
        }

        if (this.clearSearch)
        {
            this.searchField.setText("");
            this.clearSearch = false;
        }

        if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
            String s1 = this.searchField.getText().toLowerCase(Locale.ROOT);

            if (!s1.equals(this.lastSearch) && this.tradingRecipeList != null) {
                this.tradingRecipeList.searchQuery(s1, this.mc.gameSettings.advancedItemTooltips);
                this.lastSearch = s1;
                this.currentScroll = 0.0F;
                this.scrollTo(0.0F);
                this.populate = true;
            }

            return true;
        }

        return false;
    }

    private boolean checkValidKeys(int keyCode)
    {
        if (this.mc.player.inventory.getItemStack().isEmpty() && this.hoveredSlot > 0)
        {
            GameSettings settings = this.mc.gameSettings;
            for (int i = 0; i < 9; ++i)
            {
                if (settings.keyBindsHotbar[i].isActiveAndMatches(keyCode)) {
                    return true;
                }
            }

            if (this.hoveredSlot > 1) {
                return settings.keyBindDrop.isActiveAndMatches(keyCode);
            }
        }

        return false;
    }

    /**
     * Updates the gui slots ItemStack's based on scroll position.
     */
    private void scrollTo(float pos)
    {
        if (this.tradingRecipeList != null) {

            int i = this.tradingRecipeList.activeRecipeSize(); //size()
            int j = (int)((double)(pos * (float)Math.max(i - BUTTON_SPACE, 0)) + 0.5D);

            j = Math.max(0, j);
            //System.out.println("Active size: " + i);
            //System.out.println("scrollPosition: " + j);
            if (this.scrollPostion != j) {
                this.scrollPostion = j;
                this.populate = true;
            }
        }
    }
}