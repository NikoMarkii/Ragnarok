package com.niko.ragnarok.item.Armor.Renderer;

import com.niko.ragnarok.item.Armor.GradiusArmorItem;
import com.niko.ragnarok.item.Armor.Model.GradiusArmorModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * bipedHead/armorHead, bipedBody/armorBody, bipedRightArm/armorRightArm, ...
 * のようなボーン名の付け方をしていれば、GeoArmorRendererが自動でバニラの
 * 各部位（頭・胴・両腕・両足・両ブーツ）に対応付けてくれる。
 * gradius_armor.geo.jsonのボーン名がその命名規則に沿っているので、
 * ここで特別な対応付けは書く必要がない。
 */
public class GradiusArmorRenderer extends GeoArmorRenderer<GradiusArmorItem> {

    public GradiusArmorRenderer() {
        super(new GradiusArmorModel());
    }
}
