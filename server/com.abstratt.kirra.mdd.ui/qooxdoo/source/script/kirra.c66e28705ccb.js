/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)
     * Carsten Lergenmueller (carstenl)

************************************************************************ */

/**
 * A template class for cell renderer, which display images. Concrete
 * implementations must implement the method {@link #_identifyImage}.
 */
qx.Class.define("qx.ui.table.cellrenderer.AbstractImage",
{
  extend : qx.ui.table.cellrenderer.Abstract,
  type : "abstract",



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    var clazz = this.self(arguments);
    if (!clazz.stylesheet)
    {
      clazz.stylesheet = qx.bom.Stylesheet.createElement(
        ".qooxdoo-table-cell-icon {" +
        "  text-align:center;" +
        "  padding-top:1px;" +
        "}"
      );
    }
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */
  properties :
  {
    /**
     * Whether to repeat or scale the image.
     *
     * @param repeat {String}
     *   One of
     *     <code>scale</code>,
     *     <code>scale-x</code>,
     *     <code>scale-y</code>,
     *     <code>repeat</code>,
     *     <code>repeat-x</code>,
     *     <code>repeat-y</code>,
     *     <code>no-repeat</code>
    */
    repeat :
    {
      check : function(value)
      {
        var valid =
          [
            "scale",
            "scale-x",
            "scale-y",
            "repeat",
            "repeat-x",
            "repeat-y",
            "no-repeat"
          ];
        return qx.lang.Array.contains(valid, value);
      },
      init  : "no-repeat"
    }
  },


  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __defaultWidth : 16,
    __defaultHeight : 16,
    __imageData : null,

    // overridden
    _insetY : 2,

    /**
     * Identifies the Image to show. This is a template method, which must be
     * implemented by sub classes.
     *
     * @abstract
     * @param cellInfo {Map} The information about the cell.
     *          See {@link qx.ui.table.cellrenderer.Abstract#createDataCellHtml}.
     * @return {Map} A map having the following attributes:
     *           <ul>
     *           <li>
     *             "url": (type string) must be the URL of the image to show.
     *             The url given must either be managed by the {@link qx.util.ResourceManager}
     *             or pre-loaded with {@link qx.io.ImageLoader}. This is to make sure that
     *             the renderer knows the dimensions and the format of the image.
     *           </li>
     *           <li>"imageWidth": (type int) the width of the image in pixels.</li>
     *           <li>"imageHeight": (type int) the height of the image in pixels.</li>
     *           <li>"tooltip": (type string) must be the image tooltip text.</li>
     *           </ul>
     * @throws {Error} the abstract function warning.
     */
    _identifyImage : function(cellInfo) {
      throw new Error("_identifyImage is abstract");
    },


    /**
     * Retrieves the image infos.
     *
     * @param cellInfo {Map} The information about the cell.
     *          See {@link qx.ui.table.cellrenderer.Abstract#createDataCellHtml}.
     * @return {Map} Map with an "url" attribute (type string)
     *                 holding the URL of the image to show
     *                 and a "tooltip" attribute
     *                 (type string) being the tooltip text (or null if none was specified)
     */
    _getImageInfos : function(cellInfo)
    {
      // Query the subclass about image and tooltip
      var imageData = this._identifyImage(cellInfo);

      // If subclass refuses to give map, construct it with required properties
      // If no map is given, but instead a string, assume that this string is
      // the URL of the image [BUG #4289]
      if (imageData == null || typeof imageData == "string")
      {
        imageData =
        {
          url : imageData,
          tooltip : null
        };
      }

      // If sizes are not included in map given by subclass,
      // fall-back to calculated image size
      if (!imageData.imageWidth || !imageData.imageHeight)
      {
        var sizes = this.__getImageSize(imageData.url);

        imageData.imageWidth = sizes.width;
        imageData.imageHeight = sizes.height;
      }

      // Add width and height keys to map [BUG #4289]
      // - [width|height] is read by _getContentHtml()
      // - [imageWidth|imageHeight] is possibly read in legacy applications
      imageData.width = imageData.imageWidth;
      imageData.height = imageData.imageHeight;

      return imageData;
    },


    /**
     * Compute the size of the given image
     *
     * @param source {String} the image URL
     * @return {Map} A map containing the image's <code>width</code> and
     *    <code>height</code>
     */
    __getImageSize : function(source)
    {
      var ResourceManager = qx.util.ResourceManager.getInstance();
      var ImageLoader = qx.io.ImageLoader;
      var width, height;

      // Detect if the image registry knows this image
      if (ResourceManager.has(source))
      {
        width = ResourceManager.getImageWidth(source);
        height = ResourceManager.getImageHeight(source);
      }
      else if (ImageLoader.isLoaded(source))
      {
        width = ImageLoader.getWidth(source);
        height = ImageLoader.getHeight(source);
      }
      else
      {
        width = this.__defaultWidth;
        height = this.__defaultHeight;
      }

      return {width : width, height : height};
    },


    // overridden
    createDataCellHtml : function(cellInfo, htmlArr)
    {
      this.__imageData = this._getImageInfos(cellInfo);
      return this.base(arguments, cellInfo, htmlArr);
    },


    // overridden
    _getCellClass : function(cellInfo) {
      return this.base(arguments) + " qooxdoo-table-cell-icon";
    },


    // overridden
    _getContentHtml : function(cellInfo)
    {
      var content = "<div></div>";

      // set image
      if (this.__imageData.url) {
        content = qx.bom.element.Decoration.create(
          this.__imageData.url,
          this.getRepeat(),
          {
          width: this.__imageData.width + "px",
          height: this.__imageData.height + "px",
          display: qx.core.Environment.get("css.inlineblock"),
          verticalAlign: "top",
          position: "static"
        });
      };

      return content;
    },


    // overridden
    _getCellAttributes : function(cellInfo)
    {
      var tooltip = this.__imageData.tooltip;

      if (tooltip) {
        return "title='" + tooltip + "'";
      } else {
        return "";
      }
    }
  },

  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    this.__imageData = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)
     * Carsten Lergenmueller (carstenl)

************************************************************************ */

/**
 * A data cell renderer for boolean values.
 */
qx.Class.define("qx.ui.table.cellrenderer.Boolean",
{
  extend : qx.ui.table.cellrenderer.AbstractImage,




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    this.__aliasManager = qx.util.AliasManager.getInstance();

    this.initIconTrue();
    this.initIconFalse();

    // dynamic theme switch
    if (qx.core.Environment.get("qx.dyntheme")) {
      qx.theme.manager.Appearance.getInstance().addListener(
        "changeTheme", this._onChangeTheme, this
      );
    }
  },


  /*
   *****************************************************************************
     PROPERTIES
   *****************************************************************************
   */

  properties :
  {
    /**
     * The icon used to indicate the true state
     */
    iconTrue :
    {
      check : "String",
      init : "decoration/table/boolean-true.png",
      apply : "_applyIconTrue"
    },

    /**
    * The icon used to indicate the false state
    */
    iconFalse :
    {
      check : "String",
      init : "decoration/table/boolean-false.png",
      apply : "_applyIconFalse"
    }
  },


  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __iconUrlTrue : null,
    __iconUrlFalse : false,
    __aliasManager : null,


    /**
     * Handler for theme changes.
     * @signature function()
     */
    _onChangeTheme : qx.core.Environment.select("qx.dyntheme",
    {
      "true" : function() {
        this._applyIconTrue(this.getIconTrue());
        this._applyIconFalse(this.getIconFalse());
      },
      "false" : null
    }),

    // property apply
    _applyIconTrue : function(value) {
      this.__iconUrlTrue = this.__aliasManager.resolve(value);
    },


    // property apply
    _applyIconFalse : function(value) {
      this.__iconUrlFalse = this.__aliasManager.resolve(value);
    },


    // overridden
    _insetY : 5,

    // overridden
    _getCellStyle : function(cellInfo) {
      return this.base(arguments, cellInfo) + ";padding-top:4px;";
    },


    // overridden
    _identifyImage : function(cellInfo)
    {
      var imageHints =
      {
        imageWidth  : 11,
        imageHeight : 11
      };

      switch(cellInfo.value)
      {
        case true:
          imageHints.url = this.__iconUrlTrue;
          break;

        case false:
          imageHints.url = this.__iconUrlFalse;
          break;

        default:
          imageHints.url = null;
          break;
      }

      return imageHints;
    }
  },

  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function() {
    this.__aliasManager = null;
    // remove dynamic theme listener
    if (qx.core.Environment.get("qx.dyntheme")) {
      qx.theme.manager.Appearance.getInstance().removeListener(
        "changeTheme", this._onChangeTheme, this
      );
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Jonathan Weiß (jonathan_rass)
     * Tristan Koch (tristankoch)

************************************************************************ */

/**
 * The TextField is a multi-line text input field.
 */
qx.Class.define("qx.ui.form.TextArea",
{
  extend : qx.ui.form.AbstractField,



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param value {String?""} The text area's initial value
   */
  construct : function(value)
  {
    this.base(arguments, value);
    this.initWrap();

    this.addListener("mousewheel", this._onMousewheel, this);
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /** Controls whether text wrap is activated or not. */
    wrap :
    {
      check : "Boolean",
      init : true,
      apply : "_applyWrap"
    },

    // overridden
    appearance :
    {
      refine : true,
      init : "textarea"
    },

    /** Factor for scrolling the <code>TextArea</code> with the mouse wheel. */
    singleStep :
    {
      check : "Integer",
      init : 20
    },

    /** Minimal line height. On default this is set to four lines. */
    minimalLineHeight :
    {
      check : "Integer",
      apply : "_applyMinimalLineHeight",
      init : 4
    },

    /**
    * Whether the <code>TextArea</code> should automatically adjust to
    * the height of the content.
    *
    * To set the initial height, modify {@link #minHeight}. If you wish
    * to set a minHeight below four lines of text, also set
    * {@link #minimalLineHeight}. In order to limit growing to a certain
    * height, set {@link #maxHeight} respectively. Please note that
    * autoSize is ignored when the {@link #height} property is in use.
    */
    autoSize :
    {
      check : "Boolean",
      apply : "_applyAutoSize",
      init : false
    }

  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __areaClone : null,
    __areaHeight : null,
    __originalAreaHeight : null,

    // overridden
    setValue : function(value)
    {
      value = this.base(arguments, value);
      this.__autoSize();

      return value;
    },

    /**
     * Handles the mouse wheel for scrolling the <code>TextArea</code>.
     *
     * @param e {qx.event.type.MouseWheel} mouse wheel event.
     */
    _onMousewheel : function(e) {
      var contentElement = this.getContentElement();
      var scrollY = contentElement.getScrollY();

      if (qx.event.handler.MouseEmulation.ON) {
        contentElement.scrollToY(scrollY + e.getWheelDelta("y"));
      } else {
        contentElement.scrollToY(scrollY + e.getWheelDelta("y") * this.getSingleStep());
      }


      var newScrollY = contentElement.getScrollY();

      if (newScrollY != scrollY) {
        e.stop();
      }
    },

    /*
    ---------------------------------------------------------------------------
      AUTO SIZE
    ---------------------------------------------------------------------------
    */

    /**
    * Adjust height of <code>TextArea</code> so that content fits without scroll bar.
    *
    */
    __autoSize: function() {
      if (this.isAutoSize()) {
        var clone = this.__getAreaClone();

        if (clone && this.getBounds()) {

          // Remember original area height
          this.__originalAreaHeight = this.__originalAreaHeight || this._getAreaHeight();

          var scrolledHeight = this._getScrolledAreaHeight();

          // Show scroll-bar when above maxHeight, if defined
          if (this.getMaxHeight()) {
            var insets = this.getInsets();
            var innerMaxHeight = -insets.top + this.getMaxHeight() - insets.bottom;
            if (scrolledHeight > innerMaxHeight) {
                this.getContentElement().setStyle("overflowY", "auto");
            } else {
                this.getContentElement().setStyle("overflowY", "hidden");
            }
          }

          // Never shrink below original area height
          var desiredHeight = Math.max(scrolledHeight, this.__originalAreaHeight);

          // Set new height
          this._setAreaHeight(desiredHeight);

        // On init, the clone is not yet present. Try again on appear.
        } else {
          this.getContentElement().addListenerOnce("appear", function() {
            this.__autoSize();
          }, this);
        }
      }
    },

    /**
    * Get actual height of <code>TextArea</code>
    *
    * @return {Integer} Height of <code>TextArea</code>
    */
    _getAreaHeight: function() {
      return this.getInnerSize().height;
    },

    /**
    * Set actual height of <code>TextArea</code>
    *
    * @param height {Integer} Desired height of <code>TextArea</code>
    */
    _setAreaHeight: function(height) {
      if (this._getAreaHeight() !== height) {
        this.__areaHeight = height;

        qx.ui.core.queue.Layout.add(this);

        // Apply height directly. This works-around a visual glitch in WebKit
        // browsers where a line-break causes the text to be moved upwards
        // for one line. Since this change appears instantly whereas the queue
        // is computed later, a flicker is visible.
        qx.ui.core.queue.Manager.flush();

        this.__forceRewrap();
      }
    },

    /**
    * Get scrolled area height. Equals the total height of the <code>TextArea</code>,
    * as if no scroll-bar was visible.
    *
    * @return {Integer} Height of scrolled area
    */
    _getScrolledAreaHeight: function() {
      var clone = this.__getAreaClone();
      var cloneDom = clone.getDomElement();

      if (cloneDom) {

        // Clone created but not yet in DOM. Try again.
        if (!cloneDom.parentNode) {
          qx.html.Element.flush();
          return this._getScrolledAreaHeight();
        }

        // In WebKit and IE8, "wrap" must have been "soft" on DOM level before setting
        // "off" can disable wrapping. To fix, make sure wrap is toggled.
        // Otherwise, the height of an auto-size text area with wrapping
        // disabled initially is incorrectly computed as if wrapping was enabled.
        if (qx.core.Environment.get("engine.name") === "webkit" ||
            (qx.core.Environment.get("engine.name") == "mshtml")) {
          clone.setWrap(!this.getWrap(), true);
        }

        clone.setWrap(this.getWrap(), true);

        // Webkit needs overflow "hidden" in order to correctly compute height
        if (qx.core.Environment.get("engine.name") === "webkit" ||
            (qx.core.Environment.get("engine.name") == "mshtml")) {
          cloneDom.style.overflow = "hidden";
        }

        // IE >= 8 needs overflow "visible" in order to correctly compute height
        if (qx.core.Environment.get("engine.name") == "mshtml" &&
          qx.core.Environment.get("browser.documentmode") >= 8) {
          cloneDom.style.overflow = "visible";
          cloneDom.style.overflowX = "hidden";
        }

        // Update value
        clone.setValue(this.getValue() || "");

        // Force IE > 8 to update size measurements
        if (qx.core.Environment.get("engine.name") == "mshtml") {
          cloneDom.style.height = "auto";
          qx.html.Element.flush();
          cloneDom.style.height = "0";
        }

        // Recompute
        this.__scrollCloneToBottom(clone);

        if (qx.core.Environment.get("engine.name") == "mshtml" &&
            qx.core.Environment.get("browser.documentmode") == 8) {
          // Flush required for scrollTop to return correct value
          // when initial value should be taken into consideration
          if (!cloneDom.scrollTop) {
            qx.html.Element.flush();
          }
        }

        return cloneDom.scrollTop;
      }
    },

    /**
    * Returns the area clone.
    *
    * @return {Element|null} DOM Element or <code>null</code> if there is no
    * original element
    */
    __getAreaClone: function() {
      this.__areaClone = this.__areaClone || this.__createAreaClone();
      return this.__areaClone;
    },

    /**
    * Creates and prepares the area clone.
    *
    * @return {Element} Element
    */
    __createAreaClone: function() {
      var orig,
          clone,
          cloneDom,
          cloneHtml;

      orig = this.getContentElement();

      // An existing DOM element is required
      if (!orig.getDomElement()) {
        return null;
      }

      // Create DOM clone
      cloneDom = qx.bom.Element.clone(orig.getDomElement());

      // Convert to qx.html Element
      cloneHtml = new qx.html.Input("textarea");
      cloneHtml.useElement(cloneDom);
      clone = cloneHtml;

      // Push out of view
      // Zero height (i.e. scrolled area equals height)
      clone.setStyles({
        position: "absolute",
        top: 0,
        left: "-9999px",
        height: 0,
        overflow: "hidden"
      }, true);

      // Fix attributes
      clone.removeAttribute('id');
      clone.removeAttribute('name');
      clone.setAttribute("tabIndex", "-1");

      // Copy value
      clone.setValue(orig.getValue() || "");

      // Attach to DOM
      clone.insertBefore(orig);

      // Make sure scrollTop is actual height
      this.__scrollCloneToBottom(clone);

      return clone;
    },

    /**
    * Scroll <code>TextArea</code> to bottom. That way, scrollTop reflects the height
    * of the <code>TextArea</code>.
    *
    * @param clone {Element} The <code>TextArea</code> to scroll
    */
    __scrollCloneToBottom: function(clone) {
      clone = clone.getDomElement();
      if (clone) {
        clone.scrollTop = 10000;
      }
    },

    /*
    ---------------------------------------------------------------------------
      FIELD API
    ---------------------------------------------------------------------------
    */

    // overridden
    _createInputElement : function()
    {
      return new qx.html.Input("textarea", {
        overflowX: "auto",
        overflowY: "auto"
      });
    },


    /*
    ---------------------------------------------------------------------------
      APPLY ROUTINES
    ---------------------------------------------------------------------------
    */

    // property apply
    _applyWrap : function(value, old) {
      this.getContentElement().setWrap(value);
      if (this._placeholder) {
        var whiteSpace = value ? "normal" : "nowrap";
        this._placeholder.setStyle("whiteSpace", whiteSpace);
      }
      this.__autoSize();
    },

    // property apply
    _applyMinimalLineHeight : function() {
      qx.ui.core.queue.Layout.add(this);
    },

    // property apply
    _applyAutoSize: function(value, old) {
      if (qx.core.Environment.get("qx.debug")) {
        this.__warnAutoSizeAndHeight();
      }

      if (value) {
        this.__autoSize();
        this.addListener("input", this.__autoSize, this);

        // This is done asynchronously on purpose. The style given would
        // otherwise be overridden by the DOM changes queued in the
        // property apply for wrap. See [BUG #4493] for more details.
        if (!this.getBounds()) {
          this.addListenerOnce("appear", function() {
            this.getContentElement().setStyle("overflowY", "hidden");
          });
        } else {
          this.getContentElement().setStyle("overflowY", "hidden");
        }

      } else {
        this.removeListener("input", this.__autoSize);
        this.getContentElement().setStyle("overflowY", "auto");
      }
    },


    // property apply
    _applyDimension : function(value) {
      this.base(arguments);

      if (qx.core.Environment.get("qx.debug")) {
        this.__warnAutoSizeAndHeight();
      }

      if (value === this.getMaxHeight()) {
        this.__autoSize();
      }
    },

    /**
     * Force rewrapping of text.
     *
     * The distribution of characters depends on the space available.
     * Unfortunately, browsers do not reliably (or not at all) rewrap text when
     * the size of the text area changes.
     *
     * This method is called on change of the area's size.
     */
    __forceRewrap : function() {
      var content = this.getContentElement();
      var element = content.getDomElement();

      // Temporarily increase width
      var width = content.getStyle("width");
      content.setStyle("width", parseInt(width, 10) + 1000 + "px", true);

      // Force browser to render
      if (element) {
        qx.bom.element.Dimension.getWidth(element);
      }

      // Restore width
      content.setStyle("width", width, true);
    },

    /**
     * Warn when both autoSize and height property are set.
     *
     */
    __warnAutoSizeAndHeight: function() {
      if (this.isAutoSize() && this.getHeight()) {
        this.warn("autoSize is ignored when the height property is set. " +
                  "If you want to set an initial height, use the minHeight " +
                  "property instead.");
      }
    },

    /*
    ---------------------------------------------------------------------------
      LAYOUT
    ---------------------------------------------------------------------------
    */

    // overridden
    _getContentHint : function()
    {
      var hint = this.base(arguments);

      // lines of text
      hint.height = hint.height * this.getMinimalLineHeight();

      // 20 character wide
      hint.width = this._getTextSize().width * 20;

      if (this.isAutoSize()) {
        hint.height = this.__areaHeight || hint.height;
      }

      return hint;
    }
  },


  destruct : function() {
    this.setAutoSize(false);
    if (this.__areaClone) {
      this.__areaClone.dispose();
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * A toggle Button widget
 *
 * If the user presses the button by clicking on it pressing the enter or
 * space key, the button toggles between the pressed an not pressed states.
 * There is no execute event, only a {@link qx.ui.form.ToggleButton#changeValue}
 * event.
 */
qx.Class.define("qx.ui.form.ToggleButton",
{
  extend : qx.ui.basic.Atom,
  include : [
    qx.ui.core.MExecutable
  ],
  implement : [
    qx.ui.form.IBooleanForm,
    qx.ui.form.IExecutable,
    qx.ui.form.IRadioItem
  ],



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * Creates a ToggleButton.
   *
   * @param label {String} The text on the button.
   * @param icon {String} An URI to the icon of the button.
   */
  construct : function(label, icon)
  {
    this.base(arguments, label, icon);

    // register mouse events
    this.addListener("mouseover", this._onMouseOver);
    this.addListener("mouseout", this._onMouseOut);
    this.addListener("mousedown", this._onMouseDown);
    this.addListener("mouseup", this._onMouseUp);

    // register keyboard events
    this.addListener("keydown", this._onKeyDown);
    this.addListener("keyup", this._onKeyUp);

    // register execute event
    this.addListener("execute", this._onExecute, this);

  },



  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties:
  {
    // overridden
    appearance:
    {
      refine: true,
      init: "button"
    },

    // overridden
    focusable :
    {
      refine : true,
      init : true
    },

    /** The value of the widget. True, if the widget is checked. */
    value :
    {
      check : "Boolean",
      nullable : true,
      event : "changeValue",
      apply : "_applyValue",
      init : false
    },

    /** The assigned qx.ui.form.RadioGroup which handles the switching between registered buttons. */
    group :
    {
      check  : "qx.ui.form.RadioGroup",
      nullable : true,
      apply : "_applyGroup"
    },

    /**
    * Whether the button has a third state. Use this for tri-state checkboxes.
    *
    * When enabled, the value null of the property value stands for "undetermined",
    * while true is mapped to "enabled" and false to "disabled" as usual. Note
    * that the value property is set to false initially.
    *
    */
    triState :
    {
      check : "Boolean",
      apply : "_applyTriState",
      nullable : true,
      init : null
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /** The assigned {@link qx.ui.form.RadioGroup} which handles the switching between registered buttons */
    _applyGroup : function(value, old)
    {
      if (old) {
        old.remove(this);
      }

      if (value) {
        value.add(this);
      }
    },


    /**
     * Changes the state of the button dependent on the checked value.
     *
     * @param value {Boolean} Current value
     * @param old {Boolean} Previous value
     */
    _applyValue : function(value, old) {
      value ? this.addState("checked") : this.removeState("checked");

      if (this.isTriState()) {
        if (value === null) {
          this.addState("undetermined");
        } else if (old === null) {
          this.removeState("undetermined");
        }
      }
    },

    /**
    * Apply value property when triState property is modified.
    *
    * @param value {Boolean} Current value
    * @param old {Boolean} Previous value
    */
    _applyTriState : function(value, old) {
      this._applyValue(this.getValue());
    },


    /**
     * Handler for the execute event.
     *
     * @param e {qx.event.type.Event} The execute event.
     */
    _onExecute : function(e) {
      this.toggleValue();
    },


    /**
     * Listener method for "mouseover" event.
     * <ul>
     * <li>Adds state "hovered"</li>
     * <li>Removes "abandoned" and adds "pressed" state (if "abandoned" state is set)</li>
     * </ul>
     *
     * @param e {Event} Mouse event
     */
    _onMouseOver : function(e)
    {
      if (e.getTarget() !== this) {
        return;
      }

      this.addState("hovered");

      if (this.hasState("abandoned"))
      {
        this.removeState("abandoned");
        this.addState("pressed");
      }
    },


    /**
     * Listener method for "mouseout" event.
     * <ul>
     * <li>Removes "hovered" state</li>
     * <li>Adds "abandoned" state (if "pressed" state is set)</li>
     * <li>Removes "pressed" state (if "pressed" state is set and button is not checked)
     * </ul>
     *
     * @param e {Event} Mouse event
     */
    _onMouseOut : function(e)
    {
      if (e.getTarget() !== this) {
        return;
      }

      this.removeState("hovered");

      if (this.hasState("pressed"))
      {
        if (!this.getValue()) {
          this.removeState("pressed");
        }

        this.addState("abandoned");
      }
    },


    /**
     * Listener method for "mousedown" event.
     * <ul>
     * <li>Activates capturing</li>
     * <li>Removes "abandoned" state</li>
     * <li>Adds "pressed" state</li>
     * </ul>
     *
     * @param e {Event} Mouse event
     */
    _onMouseDown : function(e)
    {
      if (!e.isLeftPressed()) {
        return;
      }

      // Activate capturing if the button get a mouseout while
      // the button is pressed.
      this.capture();

      this.removeState("abandoned");
      this.addState("pressed");
      e.stopPropagation();
    },


    /**
     * Listener method for "mouseup" event.
     * <ul>
     * <li>Releases capturing</li>
     * <li>Removes "pressed" state (if not "abandoned" state is set and "pressed" state is set)</li>
     * <li>Removes "abandoned" state (if set)</li>
     * <li>Toggles {@link #value} (if state "abandoned" is not set and state "pressed" is set)</li>
     * </ul>
     *
     * @param e {Event} Mouse event
     */
    _onMouseUp : function(e)
    {
      this.releaseCapture();

      if (this.hasState("abandoned")) {
        this.removeState("abandoned");
      } else if (this.hasState("pressed")) {
        this.execute();
      }

      this.removeState("pressed");
      e.stopPropagation();
    },


    /**
     * Listener method for "keydown" event.<br/>
     * Removes "abandoned" and adds "pressed" state
     * for the keys "Enter" or "Space"
     *
     * @param e {Event} Key event
     */
    _onKeyDown : function(e)
    {
      switch(e.getKeyIdentifier())
      {
        case "Enter":
        case "Space":
          this.removeState("abandoned");
          this.addState("pressed");

          e.stopPropagation();
      }
    },


    /**
     * Listener method for "keyup" event.<br/>
     * Removes "abandoned" and "pressed" state (if "pressed" state is set)
     * for the keys "Enter" or "Space". It also toggles the {@link #value} property.
     *
     * @param e {Event} Key event
     */
    _onKeyUp : function(e)
    {
      if (!this.hasState("pressed")) {
        return;
      }

      switch(e.getKeyIdentifier())
      {
        case "Enter":
        case "Space":
          this.removeState("abandoned");
          this.execute();

          this.removeState("pressed");
          e.stopPropagation();
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)
     * Andreas Ecker (ecker)

************************************************************************ */

/**
 * A check box widget with an optional label.
 */
qx.Class.define("qx.ui.form.CheckBox",
{
  extend : qx.ui.form.ToggleButton,
  include : [
    qx.ui.form.MForm,
    qx.ui.form.MModelProperty
  ],
  implement : [
    qx.ui.form.IForm,
    qx.ui.form.IModel
  ],

  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param label {String?null} An optional label for the check box.
   */
  construct : function(label)
  {
    if (qx.core.Environment.get("qx.debug")) {
      this.assertArgumentsCount(arguments, 0, 1);
    }

    this.base(arguments, label);

    // Initialize the checkbox to a valid value (the default is null which
    // is invalid)
    this.setValue(false);
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "checkbox"
    },

    // overridden
    allowGrowX :
    {
      refine : true,
      init : false
    }
  },

  members :
  {
    /**
     * @lint ignoreReferenceField(_forwardStates)
     */
    _forwardStates :
    {
      invalid : true,
      focused : true,
      undetermined : true,
      checked : true,
      hovered : true
    },

    /**
     * overridden (from MExecutable to keep the icon out of the binding)
     * @lint ignoreReferenceField(_bindableProperties)
     */
    _bindableProperties :
    [
      "enabled",
      "label",
      "toolTipText",
      "value",
      "menu"
    ]
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2011 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Tristan Koch (tristankoch)

************************************************************************ */

/**
 * Interface of an authentication delegate.
 */
qx.Interface.define("qx.io.request.authentication.IAuthentication",
{

  members :
  {
    /**
     * Headers to include in request for authentication purposes.
     *
     * @return {Map[]} Array of maps. Each map represent a header and
     *          must have the properties <code>key</code> and <code>value</code>
     *         with a value of type string.
     */
    getAuthHeaders: function() {}
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2011 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Tristan Koch (tristankoch)

************************************************************************ */

/**
 * Basic authentication.
 */
qx.Class.define("qx.io.request.authentication.Basic",
{

  extend: qx.core.Object,

  implement: qx.io.request.authentication.IAuthentication,

  /**
   * @param username {var} The username to use.
   * @param password {var} The password to use.
   */
  construct : function(username, password)
  {
     this.__credentials = qx.util.Base64.encode(username + ':' + password);
  },

  members :
  {
    __credentials : null,

    /**
     * Headers to include for basic authentication.
     * @return {Map} Map containing the authentication credentials
     */
    getAuthHeaders: function() {
      return [
        {key: "Authorization", value: "Basic " + this.__credentials}
      ];
    }
  },

  destruct : function() {
    this.__credentials = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Adrian Olaru (adrianolaru)

************************************************************************ */

/**
 * Base64 encoder
 */
qx.Class.define("qx.util.Base64", {
  statics : {
    /** Characters allowed in a Base 64 encoded string */
    __base64Chars : [ 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' ],


    /**
     * Encode a string using base64 encoding (http://en.wikipedia.org/wiki/Base64).
     *
     * @param input {String} the input string to encode
     *
     * @param is8bit {Boolean?} Whether the character set is 8-bit, not
     *   multi-byte.  If this parameter is not provided, the character set is
     *   determined from the 'document' object.
     *
     * @return {String} The base64 encoded input string.
     */
    encode : function(input, is8bit)
    {
      var isMultiByte;

      if (typeof is8bit == "undefined")
      {
        var charSet = document.characterSet || document.charset;
        isMultiByte = charSet.toLowerCase().indexOf('utf') != -1;

        if (!isMultiByte && window.btoa instanceof Function) {
          return btoa(input);
        }
      }
      else
      {
        isMultiByte = ! is8bit;
      }

      var padding = '=';
      var base64Chars = this.__base64Chars;
      var length = input.length;
      var output = [];
      var result = [];
      var i = 0;

      var translateUTF8 = this.__translateUTF8;

      while (i < length) {
        translateUTF8(input.charCodeAt(i++), output, !isMultiByte);
      }

      for (var k=0, l=output.length; k<l; k+=3)
      {
        if (k + 1 === l)
        {
          result.push(base64Chars[output[k] >> 2]);
          result.push(base64Chars[(output[k] & 3) << 4]);
          result.push(padding + padding);
          break;
        }

        if (k + 2 === l)
        {
          result.push(base64Chars[output[k] >> 2]);
          result.push(base64Chars[(output[k] & 3) << 4 | output[k + 1] >> 4]);
          result.push(base64Chars[(output[k + 1] & 15) << 2]);
          result.push(padding);
          break;
        }

        result.push(base64Chars[output[k] >> 2]);
        result.push(base64Chars[(output[k] & 3) << 4 | output[k + 1] >> 4]);
        result.push(base64Chars[(output[k + 1] & 15) << 2 | output[k + 2] >> 6]);
        result.push(base64Chars[output[k + 2] & 63]);
      }

      return result.join('');
    },


    /**
     * Adds to output array the UTF-8 bytes needed to represent the character (http://en.wikipedia.org/wiki/UTF8)
     *
     * @param characterCode {Integer} the code of the character
     * @param output {Array} the array of bytes to be filled
     * @param is8bit {Boolean} specifies whether we should not treat the array as a multi byte string
     */
    __translateUTF8 : function(characterCode, output, is8bit)
    {
      if (characterCode < 128)
      {
        output.push(characterCode);
        return;
      }

      if (characterCode < 256 && is8bit)
      {
        output.push(characterCode);
        return;
      }

      if (characterCode < 2048)
      {
        output.push(192 | characterCode >> 6);
        output.push(128 | characterCode & 63);
        return;
      }

      if (characterCode < 65536)
      {
        output.push(224 | characterCode >> 12);
        output.push(128 | (characterCode >> 6) & 63);
        output.push(128 | characterCode & 63);
        return;
      }
      else
      {
        output.push(240 | characterCode >> 18);
        output.push(128 | (characterCode >> 12) & 63);
        output.push(128 | (characterCode >> 6) & 63);
        output.push(128 | characterCode & 63);
        return;
      }
    },


    /**
     * Returns a String from an array of bytes, with special treatment
     * if the bytes are UTF-8 bytes (http://en.wikipedia.org/wiki/UTF8)
     *
     * @param bytes {Array} the byte array [8it integers]
     * @param is8bit {Boolean} specifies whether we should not treat the array as a multi byte string
     * @return {String} the string backed by the byte array
     */
    __getUTF8StringFromBytes : function(bytes, is8bit)
    {
      var charString = '';
      var result = [];

      if (is8bit)
      {
        result = bytes;
      }
      else
      {
        for (var i=0; i<bytes.length; i++)
        {
          var utfByte = bytes[i];

          if (utfByte >> 7 === 0) {
            result.push(utfByte);
          }

          if (utfByte >> 5 === 6)
          {
            var nextByte = bytes[++i];
            result.push(((utfByte & 28) >> 2) << 8 | ((utfByte & 3) << 6) | nextByte & 63);
          }

          if (utfByte >> 4 === 14)
          {
            var nextBytes = [ bytes[++i], bytes[++i] ];
            result.push((utfByte & 15) << 12 | ((nextBytes[0] & 60) >> 2) << 8 | (nextBytes[0] & 3) << 6 | (nextBytes[1] & 63));
          }

          if (utfByte >> 3 === 30)
          {
            var nextBytes = [ bytes[++i], bytes[++i], bytes[++i] ];
            result.push((utfByte & 7) << 18 | (utfByte & 48) << 16 | (nextBytes[0] & 15) << 12 | ((nextBytes[1] & 60) >> 2) << 8 | (nextBytes[1] & 3) << 6 | (nextBytes[2] & 63));
          }
        }
      }

      for (var i=0, l=result.length; i<l; i++) {
        charString += String.fromCharCode(result[i]);
      }

      return charString;
    },


    /**
     * Decode a base64 string (http://en.wikipedia.org/wiki/Base64).
     *
     * @param input {String} the input string to decode
     *
     * @param is8bit {Boolean?} Whether the character set is 8-bit, not
     *   multi-byte.  If this parameter is not provided, the character set is
     *   determined from the 'document' object.
     *
     * @return {String} The decoded input string.
     */
    decode : function(input, is8bit)
    {
      var base64Chars = this.__base64Chars;
      var isMultiByte;

      if (typeof is8bit == "undefined")
      {
        var charSet = document.characterSet || document.charset;
        isMultiByte = charSet.toLowerCase().indexOf('utf') != -1;

        if (!isMultiByte && window.atob instanceof Function) {
          return atob(input);
        }
      }
      else
      {
        isMultiByte = ! is8bit;
      }

      var ilength = input.length;
      var stringBytes = [], i = 0;

      while (i < ilength)
      {
        var base64Byte1 = base64Chars.indexOf(input.charAt(i++));
        var base64Byte2 = base64Chars.indexOf(input.charAt(i++));
        var c1 = base64Byte1 << 2 | base64Byte2 >> 4;
        stringBytes.push(c1);
        var specialChar = input.charAt(i++);

        if (specialChar !== '=')
        {
          var base64Byte3 = base64Chars.indexOf(specialChar);
          var c2 = (base64Byte2 & 15) << 4 | (base64Byte3 & 60) >> 2;
          stringBytes.push(c2);
        }

        specialChar = input.charAt(i++);

        if (specialChar !== '=')
        {
          var base64Byte4 = base64Chars.indexOf(specialChar);
          var c3 = (base64Byte3 & 3) << 6 | base64Byte4;
          stringBytes.push(c3);
        }
      }

      return this.__getUTF8StringFromBytes(stringBytes, !isMultiByte);
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */
/**
 * This interface defines the necessary features a form renderer should have.
 * Keep in mind that all renderes has to be widgets.
 */
qx.Interface.define("qx.ui.form.renderer.IFormRenderer",
{
  members :
  {
    /**
     * Add a group of form items with the corresponding names. The names should
     * be displayed as hint for the user what to do with the form item.
     * The title is optional and can be used as grouping for the given form
     * items.
     *
     * @param items {qx.ui.core.Widget[]} An array of form items to render.
     * @param names {String[]} An array of names for the form items.
     * @param title {String?} A title of the group you are adding.
     * @param itemsOptions {Array?null} The added additional data.
     * @param headerOptions {Map?null} The options map as defined by the form
     *   for the current group header.
     */
    addItems : function(items, names, title, itemsOptions, headerOptions) {},


    /**
     * Adds a button the form renderer.
     *
     * @param button {qx.ui.form.Button} A button which should be added to
     *   the form.
     * @param options {Map?null} The added additional data.
     */
    addButton : function(button, options) {}

  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Abstract renderer for {@link qx.ui.form.Form}. This abstract renderer should
 * be the superclass of all form renderer. It takes the form, which is
 * supplied as constructor parameter and configures itself. So if you need to
 * set some additional information on your renderer before adding the widgets,
 * be sure to do that before calling this.base(arguments, form).
 */
qx.Class.define("qx.ui.form.renderer.AbstractRenderer",
{
  type : "abstract",
  extend : qx.ui.core.Widget,
  implement : qx.ui.form.renderer.IFormRenderer,

  /**
   * @param form {qx.ui.form.Form} The form to render.
   */
  construct : function(form)
  {
    this.base(arguments);

    this._visibilityBindingIds = [];
    this._labels = [];

    // translation support
    if (qx.core.Environment.get("qx.dynlocale")) {
      qx.locale.Manager.getInstance().addListener(
        "changeLocale", this._onChangeLocale, this
      );
      this._names = [];
    }

    // add the groups
    var groups = form.getGroups();
    for (var i = 0; i < groups.length; i++) {
      var group = groups[i];
      this.addItems(
        group.items, group.labels, group.title, group.options, group.headerOptions
      );
    }

    // add the buttons
    var buttons = form.getButtons();
    var buttonOptions = form.getButtonOptions();
    for (var i = 0; i < buttons.length; i++) {
      this.addButton(buttons[i], buttonOptions[i]);
    }
  },


  members :
  {
    _names : null,
    _visibilityBindingIds : null,
    _labels : null,


    /**
     * Helper to bind the item's visibility to the label's visibility.
     * @param item {qx.ui.core.Widget} The form element.
     * @param label {qx.ui.basic.Label} The label for the form element.
     */
    _connectVisibility : function(item, label) {
      // map the items visibility to the label
      var id = item.bind("visibility", label, "visibility");
      this._visibilityBindingIds.push({id: id, item: item});
    },


    /**
     * Locale change event handler
     *
     * @signature function(e)
     * @param e {Event} the change event
     */
    _onChangeLocale : qx.core.Environment.select("qx.dynlocale",
    {
      "true" : function(e) {
        for (var i = 0; i < this._names.length; i++) {
          var entry = this._names[i];
          if (entry.name && entry.name.translate) {
            entry.name = entry.name.translate();
          }
          var newText = this._createLabelText(entry.name, entry.item);
          entry.label.setValue(newText);
        };
      },

      "false" : null
    }),


    /**
     * Creates the label text for the given form item.
     *
     * @param name {String} The content of the label without the
     *   trailing * and :
     * @param item {qx.ui.form.IForm} The item, which has the required state.
     * @return {String} The text for the given item.
     */
    _createLabelText : function(name, item)
    {
      var required = "";
      if (item.getRequired()) {
       required = " <span style='color:red'>*</span> ";
      }

      // Create the label. Append a colon only if there's text to display.
      var colon = name.length > 0 || item.getRequired() ? " :" : "";
      return name + required + colon;
    },


    // interface implementation
    addItems : function(items, names, title) {
      throw new Error("Abstract method call");
    },


    // interface implementation
    addButton : function(button) {
      throw new Error("Abstract method call");
    }
  },



  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    if (qx.core.Environment.get("qx.dynlocale")) {
      qx.locale.Manager.getInstance().removeListener("changeLocale", this._onChangeLocale, this);
    }
    this._names = null;

    // remove all created lables
    for (var i=0; i < this._labels.length; i++) {
      this._labels[i].dispose();
    };

    // remove the visibility bindings
    for (var i = 0; i < this._visibilityBindingIds.length; i++) {
      var entry = this._visibilityBindingIds[i];
      entry.item.removeBinding(entry.id);
    };
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Single column renderer for {@link qx.ui.form.Form}.
 */
qx.Class.define("qx.ui.form.renderer.Single",
{
  extend : qx.ui.form.renderer.AbstractRenderer,


  construct : function(form)
  {
    var layout = new qx.ui.layout.Grid();
    layout.setSpacing(6);
    layout.setColumnFlex(0, 1);
    layout.setColumnAlign(0, "right", "top");
    this._setLayout(layout);

    this.base(arguments, form);
  },


  members :
  {
    _row : 0,
    _buttonRow : null,


    /**
     * Add a group of form items with the corresponding names. The names are
     * displayed as label.
     * The title is optional and is used as grouping for the given form
     * items.
     *
     * @param items {qx.ui.core.Widget[]} An array of form items to render.
     * @param names {String[]} An array of names for the form items.
     * @param title {String?} A title of the group you are adding.
     */
    addItems : function(items, names, title) {
      // add the header
      if (title != null) {
        this._add(
          this._createHeader(title), {row: this._row, column: 0, colSpan: 2}
        );
        this._row++;
      }

      // add the items
      for (var i = 0; i < items.length; i++) {
        var label = this._createLabel(names[i], items[i]);
        this._add(label, {row: this._row, column: 0});
        var item = items[i];
        label.setBuddy(item);
        this._add(item, {row: this._row, column: 1});
        this._row++;

        this._connectVisibility(item, label);

        // store the names for translation
        if (qx.core.Environment.get("qx.dynlocale")) {
          this._names.push({name: names[i], label: label, item: items[i]});
        }
      }
    },


    /**
     * Adds a button the form renderer. All buttons will be added in a
     * single row at the bottom of the form.
     *
     * @param button {qx.ui.form.Button} The button to add.
     */
    addButton : function(button) {
      if (this._buttonRow == null) {
        // create button row
        this._buttonRow = new qx.ui.container.Composite();
        this._buttonRow.setMarginTop(5);
        var hbox = new qx.ui.layout.HBox();
        hbox.setAlignX("right");
        hbox.setSpacing(5);
        this._buttonRow.setLayout(hbox);
        // add the button row
        this._add(this._buttonRow, {row: this._row, column: 0, colSpan: 2});
        // increase the row
        this._row++;
      }

      // add the button
      this._buttonRow.add(button);
    },


    /**
     * Returns the set layout for configuration.
     *
     * @return {qx.ui.layout.Grid} The grid layout of the widget.
     */
    getLayout : function() {
      return this._getLayout();
    },


    /**
     * Creates a label for the given form item.
     *
     * @param name {String} The content of the label without the
     *   trailing * and :
     * @param item {qx.ui.core.Widget} The item, which has the required state.
     * @return {qx.ui.basic.Label} The label for the given item.
     */
    _createLabel : function(name, item) {
      var label = new qx.ui.basic.Label(this._createLabelText(name, item));
      // store lables for disposal
      this._labels.push(label);
      label.setRich(true);
      label.setAppearance("form-renderer-label");
      return label;
    },


    /**
     * Creates a header label for the form groups.
     *
     * @param title {String} Creates a header label.
     * @return {qx.ui.basic.Label} The header for the form groups.
     */
    _createHeader : function(title) {
      var header = new qx.ui.basic.Label(title);
      // store lables for disposal
      this._labels.push(header);
      header.setFont("bold");
      if (this._row != 0) {
        header.setMarginTop(10);
      }
      header.setAlignX("left");
      return header;
    }
  },


  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */
  destruct : function()
  {
    // first, remove all buttons from the button row because they
    // should not be disposed
    if (this._buttonRow) {
      this._buttonRow.removeAll();
      this._disposeObjects("_buttonRow");
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */
/**
 * <h2>Form Controller</h2>
 *
 * *General idea*
 *
 * The form controller is responsible for connecting a form with a model. If no
 * model is given, a model can be created. This created model will fit exactly
 * to the given form and can be used for serialization. All the connections
 * between the form items and the model are handled by an internal
 * {@link qx.data.controller.Object}.
 *
 * *Features*
 *
 * * Connect a form to a model (bidirectional)
 * * Create a model for a given form
 *
 * *Usage*
 *
 * The controller only works if both a controller and a model are set.
 * Creating a model will automatically set the created model.
 *
 * *Cross reference*
 *
 * * If you want to bind single values, use {@link qx.data.controller.Object}
 * * If you want to bind a list like widget, use {@link qx.data.controller.List}
 * * If you want to bind a tree widget, use {@link qx.data.controller.Tree}
 */
qx.Class.define("qx.data.controller.Form",
{
  extend : qx.core.Object,

  /**
   * @param model {qx.core.Object | null} The model to bind the target to. The
   *   given object will be set as {@link #model} property.
   * @param target {qx.ui.form.Form | null} The form which contains the form
   *   items. The given form will be set as {@link #target} property.
   * @param selfUpdate {Boolean?false} If set to true, you need to call the
   *   {@link #updateModel} method to get the data in the form to the model.
   *   Otherwise, the data will be synced automatically on every change of
   *   the form.
   */
  construct : function(model, target, selfUpdate)
  {
    this.base(arguments);

    this._selfUpdate = !!selfUpdate;
    this.__bindingOptions = {};

    if (model != null) {
      this.setModel(model);
    }

    if (target != null) {
      this.setTarget(target);
    }
  },


  properties :
  {
    /** Data object containing the data which should be shown in the target. */
    model :
    {
      check: "qx.core.Object",
      apply: "_applyModel",
      event: "changeModel",
      nullable: true,
      dereference: true
    },


    /** The target widget which should show the data. */
    target :
    {
      check: "qx.ui.form.Form",
      apply: "_applyTarget",
      event: "changeTarget",
      nullable: true,
      init: null,
      dereference: true
    }
  },


  members :
  {
    __objectController : null,
    __bindingOptions : null,


    /**
     * The form controller uses for setting up the bindings the fundamental
     * binding layer, the {@link qx.data.SingleValueBinding}. To achieve a
     * binding in both directions, two bindings are neede. With this method,
     * you have the opportunity to set the options used for the bindings.
     *
     * @param name {String} The name of the form item for which the options
     *   should be used.
     * @param model2target {Map} Options map used for the binding from model
     *   to target. The possible options can be found in the
     *   {@link qx.data.SingleValueBinding} class.
     * @param target2model {Map} Options map used for the binding from target
     *   to model. The possible options can be found in the
     *   {@link qx.data.SingleValueBinding} class.
     */
    addBindingOptions : function(name, model2target, target2model)
    {
      this.__bindingOptions[name] = [model2target, target2model];

      // return if not both, model and target are given
      if (this.getModel() == null || this.getTarget() == null) {
        return;
      }

      // renew the affected binding
      var item = this.getTarget().getItems()[name];
      var targetProperty =
        this.__isModelSelectable(item) ? "modelSelection[0]" : "value";

      // remove the binding
      this.__objectController.removeTarget(item, targetProperty, name);
      // set up the new binding with the options
      this.__objectController.addTarget(
        item, targetProperty, name, !this._selfUpdate, model2target, target2model
      );
    },


    /**
     * Creates and sets a model using the {@link qx.data.marshal.Json} object.
     * Remember that this method can only work if the form is set. The created
     * model will fit exactly that form. Changing the form or adding an item to
     * the form will need a new model creation.
     *
     * @param includeBubbleEvents {Boolean} Whether the model should support
     *   the bubbling of change events or not.
     * @return {qx.core.Object} The created model.
     */
    createModel : function(includeBubbleEvents) {
      var target = this.getTarget();

      // throw an error if no target is set
      if (target == null) {
        throw new Error("No target is set.");
      }

      var items = target.getItems();
      var data = {};
      for (var name in items) {
        var names = name.split(".");
        var currentData = data;
        for (var i = 0; i < names.length; i++) {
          // if its the last item
          if (i + 1 == names.length) {
            // check if the target is a selection
            var clazz = items[name].constructor;
            var itemValue = null;
            if (qx.Class.hasInterface(clazz, qx.ui.core.ISingleSelection)) {
              // use the first element of the selection because passed to the
              // marshaler (and its single selection anyway) [BUG #3541]
              itemValue = items[name].getModelSelection().getItem(0) || null;
            } else {
              itemValue = items[name].getValue();
            }
            // call the converter if available [BUG #4382]
            if (this.__bindingOptions[name] && this.__bindingOptions[name][1]) {
              itemValue = this.__bindingOptions[name][1].converter(itemValue);
            }
            currentData[names[i]] = itemValue;
          } else {
            // if its not the last element, check if the object exists
            if (!currentData[names[i]]) {
              currentData[names[i]] = {};
            }
            currentData = currentData[names[i]];
          }
        }
      }

      var model = qx.data.marshal.Json.createModel(data, includeBubbleEvents);
      this.setModel(model);

      return model;
    },


    /**
     * Responsible for synching the data from entered in the form to the model.
     * Please keep in mind that this method only works if you create the form
     * with <code>selfUpdate</code> set to true. Otherwise, this method will
     * do nothing because updates will be synched automatically on every
     * change.
     */
    updateModel: function(){
      // only do stuff if self update is enabled and a model or target is set
      if (!this._selfUpdate || !this.getModel() || !this.getTarget()) {
        return;
      }

      var items = this.getTarget().getItems();
      for (var name in items) {
        var item = items[name];
        var sourceProperty =
          this.__isModelSelectable(item) ? "modelSelection[0]" : "value";

        var options = this.__bindingOptions[name];
        options = options && this.__bindingOptions[name][1];

        qx.data.SingleValueBinding.updateTarget(
          item, sourceProperty, this.getModel(), name, options
        );
      }
    },


    // apply method
    _applyTarget : function(value, old) {
      // if an old target is given, remove the binding
      if (old != null) {
        this.__tearDownBinding(old);
      }

      // do nothing if no target is set
      if (this.getModel() == null) {
        return;
      }

      // target and model are available
      if (value != null) {
        this.__setUpBinding();
      }
    },


    // apply method
    _applyModel : function(value, old) {

      // set the model to null to reset all items before removing them
      if (this.__objectController != null && value == null) {
        this.__objectController.setModel(null);
      }

      // first, get rid off all bindings (avoids wrong data population)
      if (this.__objectController != null) {
        var items = this.getTarget().getItems();
        for (var name in items) {
          var item = items[name];
          var targetProperty =
            this.__isModelSelectable(item) ? "modelSelection[0]" : "value";
          this.__objectController.removeTarget(item, targetProperty, name);
        }
      }

      // set the model of the object controller if available
      if (this.__objectController != null) {
        this.__objectController.setModel(value);
      }

      // do nothing is no target is set
      if (this.getTarget() == null) {
        return;
      }

      // model and target are available
      if (value != null) {
        this.__setUpBinding();
      }
    },


    /**
     * Internal helper for setting up the bindings using
     * {@link qx.data.controller.Object#addTarget}. All bindings are set
     * up bidirectional.
     */
    __setUpBinding : function() {
      // create the object controller
      if (this.__objectController == null) {
        this.__objectController = new qx.data.controller.Object(this.getModel());
      }

      // get the form items
      var items = this.getTarget().getItems();

      // connect all items
      for (var name in items) {
        var item = items[name];
        var targetProperty =
          this.__isModelSelectable(item) ? "modelSelection[0]" : "value";
        var options = this.__bindingOptions[name];

        // try to bind all given items in the form
        try {
          if (options == null) {
            this.__objectController.addTarget(item, targetProperty, name, !this._selfUpdate);
          } else {
            this.__objectController.addTarget(
              item, targetProperty, name, !this._selfUpdate, options[0], options[1]
            );
          }
        // ignore not working items
        } catch (ex) {
          if (qx.core.Environment.get("qx.debug")) {
            this.warn("Could not bind property " + name + " of " + this.getModel());
          }
        }
      }
      // make sure the initial values of the model are taken for resetting [BUG #5874]
      this.getTarget().redefineResetter();
    },


    /**
     * Internal helper for removing all set up bindings using
     * {@link qx.data.controller.Object#removeTarget}.
     *
     * @param oldTarget {qx.ui.form.Form} The form which has been removed.
     */
    __tearDownBinding : function(oldTarget) {
      // do nothing if the object controller has not been created
      if (this.__objectController == null) {
        return;
      }

      // get the items
      var items = oldTarget.getItems();

      // disconnect all items
      for (var name in items) {
        var item = items[name];
        var targetProperty =
          this.__isModelSelectable(item) ? "modelSelection[0]" : "value";
        this.__objectController.removeTarget(item, targetProperty, name);
      }
    },


    /**
     * Returns whether the given item implements
     * {@link qx.ui.core.ISingleSelection} and
     * {@link qx.ui.form.IModelSelection}.
     *
     * @param item {qx.ui.form.IForm} The form item to check.
     *
     * @return {Boolean} true, if given item fits.
     */
    __isModelSelectable : function(item) {
      return qx.Class.hasInterface(item.constructor, qx.ui.core.ISingleSelection) &&
      qx.Class.hasInterface(item.constructor, qx.ui.form.IModelSelection);
    }

  },



  /*
   *****************************************************************************
      DESTRUCTOR
   *****************************************************************************
   */

   destruct : function() {
     // dispose the object controller because the bindings need to be removed
     if (this.__objectController) {
       this.__objectController.dispose();
     }
   }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */


/**
 * <h2>Object Controller</h2>
 *
 * *General idea*
 *
 * The idea of the object controller is to make the binding of one model object
 * containing one or more properties as easy as possible. Therefore the
 * controller can take a model as property. Every property in that model can be
 * bound to one or more target properties. The binding will be for
 * atomic types only like Numbers, Strings, ...
 *
 * *Features*
 *
 * * Manages the bindings between the model properties and the different targets
 * * No need for the user to take care of the binding ids
 * * Can create an bidirectional binding (read- / write-binding)
 * * Handles the change of the model which means adding the old targets
 *
 * *Usage*
 *
 * The controller only can work if a model is set. If the model property is
 * null, the controller is not working. But it can be null on any time.
 *
 * *Cross reference*
 *
 * * If you want to bind a list like widget, use {@link qx.data.controller.List}
 * * If you want to bind a tree widget, use {@link qx.data.controller.Tree}
 * * If you want to bind a form widget, use {@link qx.data.controller.Form}
 */
qx.Class.define("qx.data.controller.Object",
{
  extend : qx.core.Object,


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param model {qx.core.Object?null} The model for the model property.
   */
  construct : function(model)
  {
    this.base(arguments);

    // create a map for all created binding ids
    this.__bindings = {};
    // create an array to store all current targets
    this.__targets = [];

    if (model != null) {
      this.setModel(model);
    }
  },



  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /** The model object which does have the properties for the binding. */
    model :
    {
      check: "qx.core.Object",
      event: "changeModel",
      apply: "_applyModel",
      nullable: true,
      dereference: true
    }
  },



  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    // private members
    __targets : null,
    __bindings : null,

    /**
     * Apply-method which will be called if a new model has been set.
     * All bindings will be moved to the new model.
     *
     * @param value {qx.core.Object|null} The new model.
     * @param old {qx.core.Object|null} The old model.
     */
    _applyModel: function(value, old) {
      // for every target
      for (var i = 0; i < this.__targets.length; i++) {
        // get the properties
        var targetObject = this.__targets[i][0];
        var targetProperty = this.__targets[i][1];
        var sourceProperty = this.__targets[i][2];
        var bidirectional = this.__targets[i][3];
        var options = this.__targets[i][4];
        var reverseOptions = this.__targets[i][5];

        // remove it from the old if possible
        if (old != undefined && !old.isDisposed()) {
          this.__removeTargetFrom(targetObject, targetProperty, sourceProperty, old);
        }

        // add it to the new if available
        if (value != undefined) {
          this.__addTarget(
            targetObject, targetProperty, sourceProperty, bidirectional,
            options, reverseOptions
          );
        } else {
          // in shutdown situations, it may be that something is already
          // disposed [BUG #4343]
          if (targetObject.isDisposed() || qx.core.ObjectRegistry.inShutDown) {
            continue;
          }
          // if the model is null, reset the current target
          if (targetProperty.indexOf("[") == -1) {
            targetObject["reset" + qx.lang.String.firstUp(targetProperty)]();
          } else {
            var open = targetProperty.indexOf("[");
            var index = parseInt(
              targetProperty.substring(open + 1, targetProperty.length - 1), 10
            );
            targetProperty = targetProperty.substring(0, open);
            var targetArray = targetObject["get" + qx.lang.String.firstUp(targetProperty)]();
            if (index == "last") {
              index = targetArray.length;
            }
            if (targetArray) {
              targetArray.setItem(index, null);
            }
          }
        }
      }
    },


    /**
     * Adds a new target to the controller. After adding the target, the given
     * property of the model will be bound to the targets property.
     *
     * @param targetObject {qx.core.Object} The object on which the property
     *   should be bound.
     *
     * @param targetProperty {String} The property to which the binding should
     *   go.
     *
     * @param sourceProperty {String} The name of the property in the model.
     *
     * @param bidirectional {Boolean?false} Signals if the binding should also work
     *   in the reverse direction, from the target to source.
     *
     * @param options {Map?null} The options Map used by the binding from source
     *   to target. The possible options can be found in the
     *   {@link qx.data.SingleValueBinding} class.
     *
     * @param reverseOptions {Map?null} The options used by the binding in the
     *   reverse direction. The possible options can be found in the
     *   {@link qx.data.SingleValueBinding} class.
     */
    addTarget: function(
      targetObject, targetProperty, sourceProperty,
      bidirectional, options, reverseOptions
    ) {

      // store the added target
      this.__targets.push([
        targetObject, targetProperty, sourceProperty,
        bidirectional, options, reverseOptions
      ]);

      // delegate the adding
      this.__addTarget(
        targetObject, targetProperty, sourceProperty,
        bidirectional, options, reverseOptions
      );
    },


    /**
    * Does the work for {@link #addTarget} but without saving the target
    * to the internal target registry.
    *
    * @param targetObject {qx.core.Object} The object on which the property
    *   should be bound.
    *
    * @param targetProperty {String} The property to which the binding should
    *   go.
    *
    * @param sourceProperty {String} The name of the property in the model.
    *
    * @param bidirectional {Boolean?false} Signals if the binding should also work
    *   in the reverse direction, from the target to source.
    *
    * @param options {Map?null} The options Map used by the binding from source
    *   to target. The possible options can be found in the
    *   {@link qx.data.SingleValueBinding} class.
    *
    * @param reverseOptions {Map?null} The options used by the binding in the
    *   reverse direction. The possible options can be found in the
    *   {@link qx.data.SingleValueBinding} class.
    */
    __addTarget: function(
      targetObject, targetProperty, sourceProperty,
      bidirectional, options, reverseOptions
    ) {

      // do nothing if no model is set
      if (this.getModel() == null) {
        return;
      }

      // create the binding
      var id = this.getModel().bind(
        sourceProperty, targetObject, targetProperty, options
      );
      // create the reverse binding if necessary
      var idReverse = null
      if (bidirectional) {
        idReverse = targetObject.bind(
          targetProperty, this.getModel(), sourceProperty, reverseOptions
        );
      }

      // save the binding
      var targetHash = targetObject.toHashCode();
      if (this.__bindings[targetHash] == undefined) {
        this.__bindings[targetHash] = [];
      }
      this.__bindings[targetHash].push(
        [id, idReverse, targetProperty, sourceProperty, options, reverseOptions]
      );
    },

    /**
     * Removes the target identified by the three properties.
     *
     * @param targetObject {qx.core.Object} The target object on which the
     *   binding exist.
     *
     * @param targetProperty {String} The targets property name used by the
     *   adding of the target.
     *
     * @param sourceProperty {String} The name of the property of the model.
     */
    removeTarget: function(targetObject, targetProperty, sourceProperty) {
      this.__removeTargetFrom(
        targetObject, targetProperty, sourceProperty, this.getModel()
      );

      // delete the target in the targets reference
      for (var i = 0; i < this.__targets.length; i++) {
        if (
          this.__targets[i][0] == targetObject
          && this.__targets[i][1] == targetProperty
          && this.__targets[i][2] == sourceProperty
        ) {
          this.__targets.splice(i, 1);
        }
      }
    },


    /**
     * Does the work for {@link #removeTarget} but without removing the target
     * from the internal registry.
     *
     * @param targetObject {qx.core.Object} The target object on which the
     *   binding exist.
     *
     * @param targetProperty {String} The targets property name used by the
     *   adding of the target.
     *
     * @param sourceProperty {String} The name of the property of the model.
     *
     * @param sourceObject {String} The source object from which the binding
     *   comes.
     */
    __removeTargetFrom: function(
      targetObject, targetProperty, sourceProperty, sourceObject
    ) {
      // check for not fitting targetObjects
      if (!(targetObject instanceof qx.core.Object)) {
        // just do nothing
        return;
      }

      var currentListing = this.__bindings[targetObject.toHashCode()];
      // if no binding is stored
      if (currentListing == undefined || currentListing.length == 0) {
        return;
      }

      // go threw all listings for the object
      for (var i = 0; i < currentListing.length; i++) {
        // if it is the listing
        if (
          currentListing[i][2] == targetProperty &&
          currentListing[i][3] == sourceProperty
        ) {
          // remove the binding
          var id = currentListing[i][0];
          sourceObject.removeBinding(id);
          // check for the reverse binding
          if (currentListing[i][1] != null) {
            targetObject.removeBinding(currentListing[i][1]);
          }
          // delete the entry and return
          currentListing.splice(i, 1);
          return;
        }
      }
    }
  },


  /*
   *****************************************************************************
      DESTRUCT
   *****************************************************************************
   */

  destruct : function() {
    // set the model to null to get the bindings removed
    if (this.getModel() != null && !this.getModel().isDisposed()) {
      this.setModel(null);
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Martin Wittemann (martinwittemann)
     * Jonathan Weiß (jonathan_rass)
     * Christian Hagendorn (chris_schmidt)

************************************************************************ */

/**
 * A tab view is a multi page view where only one page is visible
 * at each moment. It is possible to switch the pages using the
 * buttons rendered by each page.
 *
 * @childControl bar {qx.ui.container.SlideBar} slidebar for all tab buttons
 * @childControl pane {qx.ui.container.Stack} stack container to show one tab page
 */
qx.Class.define("qx.ui.tabview.TabView",
{
  extend : qx.ui.core.Widget,
  implement : qx.ui.core.ISingleSelection,
  include : [qx.ui.core.MContentPadding],


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */


  /**
   * @param barPosition {String} Initial bar position ({@link #barPosition})
   */
  construct : function(barPosition)
  {
    this.base(arguments);

    this.__barPositionToState = {
      top : "barTop",
      right : "barRight",
      bottom : "barBottom",
      left : "barLeft"
    };

    this._createChildControl("bar");
    this._createChildControl("pane");

    // Create manager
    var mgr = this.__radioGroup = new qx.ui.form.RadioGroup;
    mgr.setWrap(false);
    mgr.addListener("changeSelection", this._onChangeSelection, this);

    // Initialize bar position
    if (barPosition != null) {
      this.setBarPosition(barPosition);
    } else {
      this.initBarPosition();
    }
  },


  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */


  events :
  {
    /** Fires after the selection was modified */
    "changeSelection" : "qx.event.type.Data"
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */


  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "tabview"
    },

    /**
     * This property defines on which side of the TabView the bar should be positioned.
     */
    barPosition :
    {
      check : ["left", "right", "top", "bottom"],
      init : "top",
      apply : "_applyBarPosition"
    }
  },


  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */


  members :
  {
    /** @type {qx.ui.form.RadioGroup} instance containing the radio group */
    __radioGroup : null,


    /*
    ---------------------------------------------------------------------------
      WIDGET API
    ---------------------------------------------------------------------------
    */


    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "bar":
          control = new qx.ui.container.SlideBar();
          control.setZIndex(10);
          this._add(control);
          break;

        case "pane":
          control = new qx.ui.container.Stack;
          control.setZIndex(5);
          this._add(control, {flex:1});
          break;
      }

      return control || this.base(arguments, id);
    },

    /**
     * Returns the element, to which the content padding should be applied.
     *
     * @return {qx.ui.core.Widget} The content padding target.
     */
    _getContentPaddingTarget : function() {
      return this.getChildControl("pane");
    },


    /*
    ---------------------------------------------------------------------------
      CHILDREN HANDLING
    ---------------------------------------------------------------------------
    */


    /**
     * Adds a page to the tabview including its needed button
     * (contained in the page).
     *
     * @param page {qx.ui.tabview.Page} The page which should be added.
     */
    add : function(page)
    {
      if (qx.core.Environment.get("qx.debug"))
      {
        if (!(page instanceof qx.ui.tabview.Page)) {
          throw new Error("Incompatible child for TabView: " + page);
        }
      }

      var button = page.getButton();
      var bar = this.getChildControl("bar");
      var pane = this.getChildControl("pane");

      // Exclude page
      page.exclude();

      // Add button and page
      bar.add(button);
      pane.add(page);

      // Register button
      this.__radioGroup.add(button);

      // Add state to page
      page.addState(this.__barPositionToState[this.getBarPosition()]);

      // Update states
      page.addState("lastTab");
      var children = this.getChildren();
      if (children[0] == page) {
        page.addState("firstTab");
      } else {
        children[children.length-2].removeState("lastTab");
      }

      page.addListener("close", this._onPageClose, this);
    },

    /**
     * Adds a page to the tabview including its needed button
     * (contained in the page).
     *
     * @param page {qx.ui.tabview.Page} The page which should be added.
     * @param index {Integer?null} Optional position where to add the page.
     */
    addAt : function(page, index)
    {
      if (qx.core.Environment.get("qx.debug"))
      {
        if (!(page instanceof qx.ui.tabview.Page)) {
          throw new Error("Incompatible child for TabView: " + page);
        }
      }
      var children = this.getChildren();
      if(!(index == null) && index > children.length) {
        throw new Error("Index should be less than : " + children.length);
      }

      if(index == null) {
        index = children.length;
      }

      var button = page.getButton();
      var bar = this.getChildControl("bar");
      var pane = this.getChildControl("pane");

      // Exclude page
      page.exclude();

      // Add button and page
      bar.addAt(button, index);
      pane.addAt(page, index);

      // Register button
      this.__radioGroup.add(button);

      // Add state to page
      page.addState(this.__barPositionToState[this.getBarPosition()]);

      // Update states
      children = this.getChildren();
      if(index == children.length-1) {
        page.addState("lastTab");
      }

      if (children[0] == page) {
        page.addState("firstTab");
      } else {
        children[children.length-2].removeState("lastTab");
      }

      page.addListener("close", this._onPageClose, this);
    },

    /**
     * Removes a page (and its corresponding button) from the TabView.
     *
     * @param page {qx.ui.tabview.Page} The page to be removed.
     */
    remove : function(page)
    {
      var pane = this.getChildControl("pane");
      var bar = this.getChildControl("bar");
      var button = page.getButton();
      var children = pane.getChildren();

      // Try to select next page
      if (this.getSelection()[0] == page)
      {
        var index = children.indexOf(page);
        if (index == 0)
        {
          if (children[1]) {
            this.setSelection([children[1]]);
          } else {
            this.resetSelection();
          }
        }
        else
        {
          this.setSelection([children[index-1]]);
        }
      }

      // Remove the button and page
      bar.remove(button);
      pane.remove(page);

      // Remove the button from the radio group
      this.__radioGroup.remove(button);

      // Remove state from page
      page.removeState(this.__barPositionToState[this.getBarPosition()]);

      // Update states
      if (page.hasState("firstTab"))
      {
        page.removeState("firstTab");
        if (children[0]) {
          children[0].addState("firstTab");
        }
      }

      if (page.hasState("lastTab"))
      {
        page.removeState("lastTab");
        if (children.length > 0) {
          children[children.length-1].addState("lastTab");
        }
      }

      page.removeListener("close", this._onPageClose, this);
    },

    /**
     * Returns TabView's children widgets.
     *
     * @return {qx.ui.tabview.Page[]} List of children.
     */
    getChildren : function() {
      return this.getChildControl("pane").getChildren();
    },

    /**
     * Returns the position of the given page in the TabView.
     *
     * @param page {qx.ui.tabview.Page} The page to query for.
     * @return {Integer} Position of the page in the TabView.
     */
    indexOf : function(page) {
      return this.getChildControl("pane").indexOf(page);
    },


    /*
    ---------------------------------------------------------------------------
      APPLY ROUTINES
    ---------------------------------------------------------------------------
    */


    /** @type {Map} Maps the bar position to an appearance state */
    __barPositionToState : null,

    /**
     * Apply method for the placeBarOnTop-Property.
     *
     * Passes the desired value to the layout of the tabview so
     * that the layout can handle it.
     * It also sets the states to all buttons so they know the
     * position of the bar.
     *
     * @param value {Boolean} The new value.
     * @param old {Boolean} The old value.
     */
    _applyBarPosition : function(value, old)
    {
      var bar = this.getChildControl("bar");
      var pane = this.getChildControl("pane");

      var horizontal = value == "left" || value == "right";
      var reversed = value == "right" || value == "bottom";

      var layoutClass = horizontal ? qx.ui.layout.HBox : qx.ui.layout.VBox;

      var layout = this._getLayout();
      if (layout && layout instanceof layoutClass) {
        // pass
      } else {
        this._setLayout(layout = new layoutClass);
      }

      // Update reversed
      layout.setReversed(reversed);

      // Sync orientation to bar
      bar.setOrientation(horizontal ? "vertical" : "horizontal");

      // Read children
      var children = this.getChildren();

      // Toggle state to bar
      if (old)
      {
        var oldState = this.__barPositionToState[old];

        // Update bar
        bar.removeState(oldState);

        // Update pane
        pane.removeState(oldState);

        // Update pages
        for (var i=0, l=children.length; i<l; i++) {
          children[i].removeState(oldState);
        }
      }

      if (value)
      {
        var newState = this.__barPositionToState[value];

        // Update bar
        bar.addState(newState);

        // Update pane
        pane.addState(newState);

        // Update pages
        for (var i=0, l=children.length; i<l; i++) {
          children[i].addState(newState);
        }
      }
    },


    /*
    ---------------------------------------------------------------------------
      SELECTION API
    ---------------------------------------------------------------------------
    */

    /**
     * Returns an array of currently selected items.
     *
     * Note: The result is only a set of selected items, so the order can
     * differ from the sequence in which the items were added.
     *
     * @return {qx.ui.tabview.Page[]} List of items.
     */
    getSelection : function() {
      var buttons = this.__radioGroup.getSelection();
      var result = [];

      for (var i = 0; i < buttons.length; i++) {
        result.push(buttons[i].getUserData("page"));
      }

      return result;
    },

    /**
     * Replaces current selection with the given items.
     *
     * @param items {qx.ui.tabview.Page[]} Items to select.
     * @throws {Error} if one of the items is not a child element and if
     *    items contains more than one elements.
     */
    setSelection : function(items) {
      var buttons = []

      for (var i = 0; i < items.length; i++) {
        buttons.push(items[i].getChildControl("button"));
      }
      this.__radioGroup.setSelection(buttons);
    },

    /**
     * Clears the whole selection at once.
     */
    resetSelection : function() {
      this.__radioGroup.resetSelection();
    },

    /**
     * Detects whether the given item is currently selected.
     *
     * @param item {qx.ui.tabview.Page} Any valid selectable item.
     * @return {Boolean} Whether the item is selected.
     * @throws {Error} if one of the items is not a child element.
     */
    isSelected : function(item) {
      var button = item.getChildControl("button");
      return this.__radioGroup.isSelected(button);
    },

    /**
     * Whether the selection is empty.
     *
     * @return {Boolean} Whether the selection is empty.
     */
    isSelectionEmpty : function() {
      return this.__radioGroup.isSelectionEmpty();
    },


    /**
     * Returns all elements which are selectable.
     *
     * @return {qx.ui.tabview.Page[]} The contained items.
     * @param all {Boolean} true for all selectables, false for the
     *   selectables the user can interactively select
     */
    getSelectables: function(all) {
      var buttons = this.__radioGroup.getSelectables(all);
      var result = [];

      for (var i = 0; i <buttons.length; i++) {
        result.push(buttons[i].getUserData("page"));
      }

      return result;
    },

    /**
     * Event handler for <code>changeSelection</code>.
     *
     * @param e {qx.event.type.Data} Data event.
     */
    _onChangeSelection : function(e)
    {
      var pane = this.getChildControl("pane");
      var button = e.getData()[0];
      var oldButton = e.getOldData()[0];
      var value = [];
      var old = [];

      if (button)
      {
        value = [button.getUserData("page")];
        pane.setSelection(value);
        button.focus();
        this.scrollChildIntoView(button, null, null, false);
      }
      else
      {
        pane.resetSelection();
      }

      if (oldButton) {
        old = [oldButton.getUserData("page")];
      }

      this.fireDataEvent("changeSelection", value, old);
    },

    /**
     * Event handler for <code>beforeChangeSelection</code>.
     *
     * @param e {qx.event.type.Event} Data event.
     */
    _onBeforeChangeSelection : function(e)
    {
      if (!this.fireNonBubblingEvent("beforeChangeSelection",
          qx.event.type.Event, [false, true])) {
        e.preventDefault();
      }
    },


    /*
    ---------------------------------------------------------------------------
      EVENT LISTENERS
    ---------------------------------------------------------------------------
    */


    /**
     * Event handler for the change of the selected item of the radio group.
     * @param e {qx.event.type.Data} The data event
     */
    _onRadioChangeSelection : function(e) {
      var element = e.getData()[0];
      if (element) {
        this.setSelection([element.getUserData("page")]);
      } else {
        this.resetSelection();
      }
    },


    /**
     * Removes the Page widget on which the close button was clicked.
     *
     * @param e {qx.event.type.Mouse} mouse click event
     */
    _onPageClose : function(e)
    {
      // reset the old close button states, before remove page
      // see http://bugzilla.qooxdoo.org/show_bug.cgi?id=3763 for details
      var page = e.getTarget()
      var closeButton = page.getButton().getChildControl("close-button");
      closeButton.reset();

      this.remove(page);
    }
  },


  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */


  destruct : function() {
    this._disposeObjects("__radioGroup");
    this.__barPositionToState = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)
     * Christian Hagendorn (chris_schmidt)
     * Adrian Olaru (adrianolaru)

************************************************************************ */

/**
 * The stack container puts its child widgets on top of each other and only the
 * topmost widget is visible.
 *
 * This is used e.g. in the tab view widget. Which widget is visible can be
 * controlled by using the {@link #getSelection} method.
 *
 * *Example*
 *
 * Here is a little example of how to use the widget.
 *
 * <pre class='javascript'>
 *   // create stack container
 *   var stack = new qx.ui.container.Stack();
 *
 *   // add some children
 *   stack.add(new qx.ui.core.Widget().set({
 *    backgroundColor: "red"
 *   }));
 *   stack.add(new qx.ui.core.Widget().set({
 *    backgroundColor: "green"
 *   }));
 *   stack.add(new qx.ui.core.Widget().set({
 *    backgroundColor: "blue"
 *   }));
 *
 *   // select green widget
 *   stack.setSelection([stack.getChildren()[1]]);
 *
 *   this.getRoot().add(stack);
 * </pre>
 *
 * This example creates an stack with three children. Only the selected "green"
 * widget is visible.
 *
 * *External Documentation*
 *
 * <a href='http://manual.qooxdoo.org/${qxversion}/pages/widget/stack.html' target='_blank'>
 * Documentation of this widget in the qooxdoo manual.</a>
 */
qx.Class.define("qx.ui.container.Stack",
{
  extend : qx.ui.core.Widget,
  implement : qx.ui.core.ISingleSelection,
  include : [
    qx.ui.core.MSingleSelectionHandling,
    qx.ui.core.MChildrenHandling
  ],


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */


  construct : function()
  {
    this.base(arguments);

    this._setLayout(new qx.ui.layout.Grow);

    this.addListener("changeSelection", this.__onChangeSelection, this);
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /**
     * Whether the size of the widget depends on the selected child. When
     * disabled (default) the size is configured to the largest child.
     */
    dynamic :
    {
      check : "Boolean",
      init : false,
      apply : "_applyDynamic"
    }
  },


  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */


  members :
  {
    // property apply
    _applyDynamic : function(value)
    {
      var children = this._getChildren();
      var selected = this.getSelection()[0];
      var child;

      for (var i=0, l=children.length; i<l; i++)
      {
        child = children[i];

        if (child != selected)
        {
          if (value) {
            children[i].exclude();
          } else {
            children[i].hide();
          }
        }
      }
    },


    /*
    ---------------------------------------------------------------------------
      HELPER METHODS FOR SELECTION API
    ---------------------------------------------------------------------------
    */


    /**
     * Returns the widget for the selection.
     * @return {qx.ui.core.Widget[]} Widgets to select.
     */
    _getItems : function() {
      return this.getChildren();
    },

    /**
     * Returns if the selection could be empty or not.
     *
     * @return {Boolean} <code>true</code> If selection could be empty,
     *    <code>false</code> otherwise.
     */
    _isAllowEmptySelection : function() {
      return true;
    },

    /**
     * Returns whether the given item is selectable.
     *
     * @param item {qx.ui.core.Widget} The item to be checked
     * @return {Boolean} Whether the given item is selectable
     */
    _isItemSelectable : function(item) {
      return true;
    },

    /**
     * Event handler for <code>changeSelection</code>.
     *
     * Shows the new selected widget and hide the old one.
     *
     * @param e {qx.event.type.Data} Data event.
     */
    __onChangeSelection : function(e)
    {
      var old = e.getOldData()[0];
      var value = e.getData()[0];

      if (old)
      {
        if (this.isDynamic()) {
          old.exclude();
        } else {
          old.hide();
        }
      }

      if (value) {
        value.show();
      }
    },


    //overriden
    _afterAddChild : function(child) {
      var selected = this.getSelection()[0];

      if (!selected) {
        this.setSelection([child]);
      } else if (selected !== child) {
        if (this.isDynamic()) {
          child.exclude();
        } else {
          child.hide();
        }
      }
    },


    //overriden
    _afterRemoveChild : function(child) {
      if (this.getSelection()[0] === child) {
        var first = this._getChildren()[0];

        if (first) {
          this.setSelection([first]);
        } else {
          this.resetSelection();
        }
      }
    },


    /*
    ---------------------------------------------------------------------------
      PUBLIC API
    ---------------------------------------------------------------------------
    */

    /**
     * Go to the previous child in the children list.
     */
    previous : function()
    {
      var selected = this.getSelection()[0];
      var go = this._indexOf(selected)-1;
      var children = this._getChildren();

      if (go < 0) {
        go = children.length - 1;
      }

      var prev = children[go];
      this.setSelection([prev]);
    },

    /**
     * Go to the next child in the children list.
     */
    next : function()
    {
      var selected = this.getSelection()[0];
      var go = this._indexOf(selected)+1;
      var children = this._getChildren();

      var next = children[go] || children[0];

      this.setSelection([next]);
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Form interface for all widgets which deal with ranges. The spinner is a good
 * example for a range using widget.
 */
qx.Interface.define("qx.ui.form.IRange",
{

  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
    ---------------------------------------------------------------------------
      MINIMUM PROPERTY
    ---------------------------------------------------------------------------
    */

    /**
     * Set the minimum value of the range.
     *
     * @param min {Number} The minimum.
     */
    setMinimum : function(min) {
      return arguments.length == 1;
    },


    /**
     * Return the current set minimum of the range.
     *
     * @return {Number} The current set minimum.
     */
    getMinimum : function() {},


    /*
    ---------------------------------------------------------------------------
      MAXIMUM PROPERTY
    ---------------------------------------------------------------------------
    */

    /**
     * Set the maximum value of the range.
     *
     * @param max {Number} The maximum.
     */
    setMaximum : function(max) {
      return arguments.length == 1;
    },


    /**
     * Return the current set maximum of the range.
     *
     * @return {Number} The current set maximum.
     */
    getMaximum : function() {},


    /*
    ---------------------------------------------------------------------------
      SINGLESTEP PROPERTY
    ---------------------------------------------------------------------------
    */

    /**
     * Sets the value for single steps in the range.
     *
     * @param step {Number} The value of the step.
     */
    setSingleStep : function(step) {
      return arguments.length == 1;
    },


    /**
     * Returns the value which will be stepped in a single step in the range.
     *
     * @return {Number} The current value for single steps.
     */
    getSingleStep : function() {},


    /*
    ---------------------------------------------------------------------------
      PAGESTEP PROPERTY
    ---------------------------------------------------------------------------
    */

    /**
     * Sets the value for page steps in the range.
     *
     * @param step {Number} The value of the step.
     */
    setPageStep : function(step) {
      return arguments.length == 1;
    },


    /**
     * Returns the value which will be stepped in a page step in the range.
     *
     * @return {Number} The current value for page steps.
     */
    getPageStep : function() {}
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Form interface for all form widgets which use a numeric value as their
 * primary data type like a spinner.
 */
qx.Interface.define("qx.ui.form.INumberForm",
{
  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events :
  {
    /** Fired when the value was modified */
    "changeValue" : "qx.event.type.Data"
  },



  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
    ---------------------------------------------------------------------------
      VALUE PROPERTY
    ---------------------------------------------------------------------------
    */

    /**
     * Sets the element's value.
     *
     * @param value {Number|null} The new value of the element.
     */
    setValue : function(value) {
      return arguments.length == 1;
    },


    /**
     * Resets the element's value to its initial value.
     */
    resetValue : function() {},


    /**
     * The element's user set value.
     *
     * @return {Number|null} The value.
     */
    getValue : function() {}
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Martin Wittemann (martinwittemann)
     * Jonathan Weiß (jonathan_rass)

************************************************************************ */

/**
 * A *spinner* is a control that allows you to adjust a numerical value,
 * typically within an allowed range. An obvious example would be to specify the
 * month of a year as a number in the range 1 - 12.
 *
 * To do so, a spinner encompasses a field to display the current value (a
 * textfield) and controls such as up and down buttons to change that value. The
 * current value can also be changed by editing the display field directly, or
 * using mouse wheel and cursor keys.
 *
 * An optional {@link #numberFormat} property allows you to control the format of
 * how a value can be entered and will be displayed.
 *
 * A brief, but non-trivial example:
 *
 * <pre class='javascript'>
 * var s = new qx.ui.form.Spinner();
 * s.set({
 *   maximum: 3000,
 *   minimum: -3000
 * });
 * var nf = new qx.util.format.NumberFormat();
 * nf.setMaximumFractionDigits(2);
 * s.setNumberFormat(nf);
 * </pre>
 *
 * A spinner instance without any further properties specified in the
 * constructor or a subsequent *set* command will appear with default
 * values and behaviour.
 *
 * @childControl textfield {qx.ui.form.TextField} holds the current value of the spinner
 * @childControl upbutton {qx.ui.form.Button} button to increase the value
 * @childControl downbutton {qx.ui.form.Button} button to decrease the value
 *
 */
qx.Class.define("qx.ui.form.Spinner",
{
  extend : qx.ui.core.Widget,
  implement : [
    qx.ui.form.INumberForm,
    qx.ui.form.IRange,
    qx.ui.form.IForm
  ],
  include : [
    qx.ui.core.MContentPadding,
    qx.ui.form.MForm
  ],


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param min {Number} Minimum value
   * @param value {Number} Current value
   * @param max {Number} Maximum value
   */
  construct : function(min, value, max)
  {
    this.base(arguments);

    // MAIN LAYOUT
    var layout = new qx.ui.layout.Grid();
    layout.setColumnFlex(0, 1);
    layout.setRowFlex(0,1);
    layout.setRowFlex(1,1);
    this._setLayout(layout);

    // EVENTS
    this.addListener("keydown", this._onKeyDown, this);
    this.addListener("keyup", this._onKeyUp, this);
    if (!(qx.event.handler.MouseEmulation.ON)) {
      this.addListener("mousewheel", this._onMouseWheel, this);
    }


    if (qx.core.Environment.get("qx.dynlocale")) {
      qx.locale.Manager.getInstance().addListener("changeLocale", this._onChangeLocale, this);
    }

    // CREATE CONTROLS
    this._createChildControl("textfield");
    this._createChildControl("upbutton");
    this._createChildControl("downbutton");

    // INITIALIZATION
    if (min != null) {
      this.setMinimum(min);
    }

    if (max != null) {
      this.setMaximum(max);
    }

    if (value !== undefined) {
      this.setValue(value);
    } else {
      this.initValue();
    }
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties:
  {
    // overridden
    appearance:
    {
      refine : true,
      init : "spinner"
    },

    // overridden
    focusable :
    {
      refine : true,
      init : true
    },

    /** The amount to increment on each event (keypress or mousedown) */
    singleStep:
    {
      check : "Number",
      init : 1
    },

    /** The amount to increment on each pageup/pagedown keypress */
    pageStep:
    {
      check : "Number",
      init : 10
    },

    /** minimal value of the Range object */
    minimum:
    {
      check : "Number",
      apply : "_applyMinimum",
      init : 0,
      event: "changeMinimum"
    },

    /** The value of the spinner. */
    value:
    {
      check : "this._checkValue(value)",
      nullable : true,
      apply : "_applyValue",
      init : 0,
      event : "changeValue"
    },

    /** maximal value of the Range object */
    maximum:
    {
      check : "Number",
      apply : "_applyMaximum",
      init : 100,
      event: "changeMaximum"
    },

    /** whether the value should wrap around */
    wrap:
    {
      check : "Boolean",
      init : false,
      apply : "_applyWrap"
    },

    /** Controls whether the textfield of the spinner is editable or not */
    editable :
    {
      check : "Boolean",
      init : true,
      apply : "_applyEditable"
    },

    /** Controls the display of the number in the textfield */
    numberFormat :
    {
      check : "qx.util.format.NumberFormat",
      apply : "_applyNumberFormat",
      nullable : true
    },

    // overridden
    allowShrinkY :
    {
      refine : true,
      init : false
    }
  },



  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /** Saved last value in case invalid text is entered */
    __lastValidValue : null,

    /** Whether the page-up button has been pressed */
    __pageUpMode : false,

    /** Whether the page-down button has been pressed */
    __pageDownMode : false,


    /*
    ---------------------------------------------------------------------------
      WIDGET INTERNALS
    ---------------------------------------------------------------------------
    */

    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "textfield":
          control = new qx.ui.form.TextField();
          control.setFilter(this._getFilterRegExp());
          control.addState("inner");
          control.setWidth(40);
          control.setFocusable(false);
          control.addListener("changeValue", this._onTextChange, this);

          this._add(control, {column: 0, row: 0, rowSpan: 2});
          break;

        case "upbutton":
          control = new qx.ui.form.RepeatButton();
          control.addState("inner");
          control.setFocusable(false);
          control.addListener("execute", this._countUp, this);
          this._add(control, {column: 1, row: 0});
          break;

        case "downbutton":
          control = new qx.ui.form.RepeatButton();
          control.addState("inner");
          control.setFocusable(false);
          control.addListener("execute", this._countDown, this);
          this._add(control, {column:1, row: 1});
          break;
      }

      return control || this.base(arguments, id);
    },


    /**
     * Returns the regular expression used as the text field's filter
     *
     * @return {RegExp} The filter RegExp.
     */
    _getFilterRegExp : function()
    {
      var decimalSeparator = qx.locale.Number.getDecimalSeparator(
        qx.locale.Manager.getInstance().getLocale()
      );
      var groupSeparator = qx.locale.Number.getGroupSeparator(
        qx.locale.Manager.getInstance().getLocale()
      );

      var prefix = "";
      var postfix = "";
      if (this.getNumberFormat() !== null) {
        prefix = this.getNumberFormat().getPrefix() || "";
        postfix = this.getNumberFormat().getPostfix() || "";
      }

      var filterRegExp = new RegExp("[0-9" +
        qx.lang.String.escapeRegexpChars(decimalSeparator) +
        qx.lang.String.escapeRegexpChars(groupSeparator) +
        qx.lang.String.escapeRegexpChars(prefix) +
        qx.lang.String.escapeRegexpChars(postfix) +
        "\-]"
      );

      return filterRegExp;
    },


    // overridden
    /**
     * @lint ignoreReferenceField(_forwardStates)
     */
    _forwardStates : {
      focused : true,
      invalid : true
    },


    // overridden
    tabFocus : function()
    {
      var field = this.getChildControl("textfield");

      field.getFocusElement().focus();
      field.selectAllText();
    },





    /*
    ---------------------------------------------------------------------------
      APPLY METHODS
    ---------------------------------------------------------------------------
    */

    /**
     * Apply routine for the minimum property.
     *
     * It sets the value of the spinner to the maximum of the current spinner
     * value and the given min property value.
     *
     * @param value {Number} The new value of the min property
     * @param old {Number} The old value of the min property
     */
    _applyMinimum : function(value, old)
    {
      if (this.getMaximum() < value) {
        this.setMaximum(value);
      }

      if (this.getValue() < value) {
        this.setValue(value);
      } else {
        this._updateButtons();
      }
    },


    /**
     * Apply routine for the maximum property.
     *
     * It sets the value of the spinner to the minimum of the current spinner
     * value and the given max property value.
     *
     * @param value {Number} The new value of the max property
     * @param old {Number} The old value of the max property
     */
    _applyMaximum : function(value, old)
    {
      if (this.getMinimum() > value) {
        this.setMinimum(value);
      }

      if (this.getValue() > value) {
        this.setValue(value);
      } else {
        this._updateButtons();
      }
    },


    // overridden
    _applyEnabled : function(value, old)
    {
      this.base(arguments, value, old);

      this._updateButtons();
    },


    /**
     * Check whether the value being applied is allowed.
     *
     * If you override this to change the allowed type, you will also
     * want to override {@link #_applyValue}, {@link #_applyMinimum},
     * {@link #_applyMaximum}, {@link #_countUp}, {@link #_countDown}, and
     * {@link #_onTextChange} methods as those cater specifically to numeric
     * values.
     *
     * @param value {var}
     *   The value being set
     * @return {Boolean}
     *   <i>true</i> if the value is allowed;
     *   <i>false> otherwise.
     */
    _checkValue : function(value) {
      return typeof value === "number" && value >= this.getMinimum() && value <= this.getMaximum();
    },


    /**
     * Apply routine for the value property.
     *
     * It checks the min and max values, disables / enables the
     * buttons and handles the wrap around.
     *
     * @param value {Number} The new value of the spinner
     * @param old {Number} The former value of the spinner
     */
    _applyValue: function(value, old)
    {
      var textField = this.getChildControl("textfield");

      this._updateButtons();

      // save the last valid value of the spinner
      this.__lastValidValue = value;

      // write the value of the spinner to the textfield
      if (value !== null) {
        if (this.getNumberFormat()) {
          textField.setValue(this.getNumberFormat().format(value));
        } else {
          textField.setValue(value + "");
        }
      } else {
        textField.setValue("");
      }
    },


    /**
     * Apply routine for the editable property.<br/>
     * It sets the textfield of the spinner to not read only.
     *
     * @param value {Boolean} The new value of the editable property
     * @param old {Boolean} The former value of the editable property
     */
    _applyEditable : function(value, old)
    {
      var textField = this.getChildControl("textfield");

      if (textField) {
        textField.setReadOnly(!value);
      }
    },


    /**
     * Apply routine for the wrap property.<br/>
     * Enables all buttons if the wrapping is enabled.
     *
     * @param value {Boolean} The new value of the wrap property
     * @param old {Boolean} The former value of the wrap property
     */
    _applyWrap : function(value, old)
    {
      this._updateButtons();
    },


    /**
     * Apply routine for the numberFormat property.<br/>
     * When setting a number format, the display of the
     * value in the textfield will be changed immediately.
     *
     * @param value {Boolean} The new value of the numberFormat property
     * @param old {Boolean} The former value of the numberFormat property
     */
    _applyNumberFormat : function(value, old) {
      var textfield = this.getChildControl("textfield");
      textfield.setFilter(this._getFilterRegExp());

      this.getNumberFormat().addListener("changeNumberFormat",
        this._onChangeNumberFormat, this);

      this._applyValue(this.__lastValidValue, undefined);
    },

    /**
     * Returns the element, to which the content padding should be applied.
     *
     * @return {qx.ui.core.Widget} The content padding target.
     */
    _getContentPaddingTarget : function() {
      return this.getChildControl("textfield");
    },

    /**
     * Checks the min and max values, disables / enables the
     * buttons and handles the wrap around.
     */
    _updateButtons : function() {
      var upButton = this.getChildControl("upbutton");
      var downButton = this.getChildControl("downbutton");
      var value = this.getValue();

      if (!this.getEnabled())
      {
        // If Spinner is disabled -> disable buttons
        upButton.setEnabled(false);
        downButton.setEnabled(false);
      }
      else
      {
        if (this.getWrap())
        {
          // If wraped -> always enable buttons
          upButton.setEnabled(true);
          downButton.setEnabled(true);
        }
        else
        {
          // check max value
          if (value !== null && value < this.getMaximum()) {
            upButton.setEnabled(true);
          } else {
            upButton.setEnabled(false);
          }

          // check min value
          if (value !== null && value > this.getMinimum()) {
            downButton.setEnabled(true);
          } else {
            downButton.setEnabled(false);
          }
        }
      }
    },

    /*
    ---------------------------------------------------------------------------
      KEY EVENT-HANDLING
    ---------------------------------------------------------------------------
    */

    /**
     * Callback for "keyDown" event.<br/>
     * Controls the interval mode ("single" or "page")
     * and the interval increase by detecting "Up"/"Down"
     * and "PageUp"/"PageDown" keys.<br/>
     * The corresponding button will be pressed.
     *
     * @param e {qx.event.type.KeySequence} keyDown event
     */
    _onKeyDown: function(e)
    {
      switch(e.getKeyIdentifier())
      {
        case "PageUp":
          // mark that the spinner is in page mode and process further
          this.__pageUpMode = true;

        case "Up":
          this.getChildControl("upbutton").press();
          break;

        case "PageDown":
          // mark that the spinner is in page mode and process further
          this.__pageDownMode = true;

        case "Down":
          this.getChildControl("downbutton").press();
          break;

        default:
          // Do not stop unused events
          return;
      }

      e.stopPropagation();
      e.preventDefault();
    },


    /**
     * Callback for "keyUp" event.<br/>
     * Detecting "Up"/"Down" and "PageUp"/"PageDown" keys.<br/>
     * Releases the button and disabled the page mode, if necessary.
     *
     * @param e {qx.event.type.KeySequence} keyUp event
     */
    _onKeyUp: function(e)
    {
      switch(e.getKeyIdentifier())
      {
        case "PageUp":
          this.getChildControl("upbutton").release();
          this.__pageUpMode = false;
          break;

        case "Up":
          this.getChildControl("upbutton").release();
          break;

        case "PageDown":
          this.getChildControl("downbutton").release();
          this.__pageDownMode = false;
          break;

        case "Down":
          this.getChildControl("downbutton").release();
          break;
      }
    },




    /*
    ---------------------------------------------------------------------------
      OTHER EVENT HANDLERS
    ---------------------------------------------------------------------------
    */

    /**
     * Callback method for the "mouseWheel" event.<br/>
     * Increments or decrements the value of the spinner.
     *
     * @param e {qx.event.type.Mouse} mouseWheel event
     */
    _onMouseWheel: function(e)
    {
      var delta = e.getWheelDelta("y");
      if (delta > 0) {
        this._countDown();
      } else if (delta < 0) {
        this._countUp();
      }

      e.stop();
    },


    /**
     * Callback method for the "change" event of the textfield.
     *
     * @param e {qx.event.type.Event} text change event or blur event
     */
    _onTextChange : function(e)
    {
      var textField = this.getChildControl("textfield");
      var value;

      // if a number format is set
      if (this.getNumberFormat())
      {
        // try to parse the current number using the number format
        try {
          value = this.getNumberFormat().parse(textField.getValue());
        } catch(ex) {
          // otherwise, process further
        }
      }

      if (value === undefined)
      {
        // try to parse the number as a float
        value = parseFloat(textField.getValue());
      }

      // if the result is a number
      if (!isNaN(value))
      {
        // Fix range
        if (value > this.getMaximum()) {
          textField.setValue(this.getMaximum() + "");
          return;
        } else if (value < this.getMinimum()) {
          textField.setValue(this.getMinimum() + "");
          return;
        }

        // set the value in the spinner
        this.setValue(value);
      }
      else
      {
        // otherwise, reset the last valid value
        this._applyValue(this.__lastValidValue, undefined);
      }
    },


    /**
     * Callback method for the locale Manager's "changeLocale" event.
     *
     * @param ev {qx.event.type.Event} locale change event
     */

    _onChangeLocale : function(ev)
    {
      if (this.getNumberFormat() !== null) {
        this.setNumberFormat(this.getNumberFormat());
        var textfield = this.getChildControl("textfield");
        textfield.setFilter(this._getFilterRegExp());
        textfield.setValue(this.getNumberFormat().format(this.getValue()));
      }
    },


    /**
     * Callback method for the number format's "changeNumberFormat" event.
     *
     * @param ev {qx.event.type.Event} number format change event
     */
    _onChangeNumberFormat : function(ev) {
      var textfield = this.getChildControl("textfield");
      textfield.setFilter(this._getFilterRegExp());
      textfield.setValue(this.getNumberFormat().format(this.getValue()));
    },




    /*
    ---------------------------------------------------------------------------
      INTERVAL HANDLING
    ---------------------------------------------------------------------------
    */

    /**
     * Checks if the spinner is in page mode and counts either the single
     * or page Step up.
     *
     */
    _countUp: function()
    {
      if (this.__pageUpMode) {
        var newValue = this.getValue() + this.getPageStep();
      } else {
        var newValue = this.getValue() + this.getSingleStep();
      }

      // handle the case where wrapping is enabled
      if (this.getWrap())
      {
        if (newValue > this.getMaximum())
        {
          var diff = this.getMaximum() - newValue;
          newValue = this.getMinimum() - diff - 1;
        }
      }

      this.gotoValue(newValue);
    },


    /**
     * Checks if the spinner is in page mode and counts either the single
     * or page Step down.
     *
     */
    _countDown: function()
    {
      if (this.__pageDownMode) {
        var newValue = this.getValue() - this.getPageStep();
      } else {
        var newValue = this.getValue() - this.getSingleStep();
      }

      // handle the case where wrapping is enabled
      if (this.getWrap())
      {
        if (newValue < this.getMinimum())
        {
          var diff = this.getMinimum() + newValue;
          newValue = this.getMaximum() + diff + 1;
        }
      }

      this.gotoValue(newValue);
    },


    /**
     * Normalizes the incoming value to be in the valid range and
     * applies it to the {@link #value} afterwards.
     *
     * @param value {Number} Any number
     * @return {Number} The normalized number
     */
    gotoValue : function(value) {
      return this.setValue(Math.min(this.getMaximum(), Math.max(this.getMinimum(), value)));
    }
  },


  destruct : function()
  {
    if (qx.core.Environment.get("qx.dynlocale")) {
      qx.locale.Manager.getInstance().removeListener("changeLocale", this._onChangeLocale, this);
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)
     * Sebastian Werner (wpbasti)
     * Jonathan Weiß (jonathan_rass)

************************************************************************ */

/**
 * Basic class for a selectbox like lists. Basically supports a popup
 * with a list and the whole children management.
 *
 * @childControl list {qx.ui.form.List} list component of the selectbox
 * @childControl popup {qx.ui.popup.Popup} popup which shows the list
 *
 */
qx.Class.define("qx.ui.form.AbstractSelectBox",
{
  extend  : qx.ui.core.Widget,
  include : [
    qx.ui.core.MRemoteChildrenHandling,
    qx.ui.form.MForm
  ],
  implement : [
    qx.ui.form.IForm
  ],
  type : "abstract",



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    // set the layout
    var layout = new qx.ui.layout.HBox();
    this._setLayout(layout);
    layout.setAlignY("middle");

    // Register listeners
    this.addListener("keypress", this._onKeyPress);
    this.addListener("blur", this._onBlur, this);

    // register mouse wheel listener
    var root = qx.core.Init.getApplication().getRoot();
    // @depreacted{3.5} Mouse wheel
    root.addListener("mousewheel", this._onMousewheel, this, true);

    // register the resize listener
    this.addListener("resize", this._onResize, this);
  },



  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    // overridden
    focusable :
    {
      refine : true,
      init : true
    },

    // overridden
    width :
    {
      refine : true,
      init : 120
    },

    /**
     * The maximum height of the list popup. Setting this value to
     * <code>null</code> will set cause the list to be auto-sized.
     */
    maxListHeight :
    {
      check : "Number",
      apply : "_applyMaxListHeight",
      nullable: true,
      init : 200
    },

    /**
     * Formatter which format the value from the selected <code>ListItem</code>.
     * Uses the default formatter {@link #_defaultFormat}.
     */
    format :
    {
      check : "Function",
      init : function(item) {
        return this._defaultFormat(item);
      },
      nullable : true
    }
  },



  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "list":
          control = new qx.ui.form.List().set({
            focusable: false,
            keepFocus: true,
            height: null,
            width: null,
            maxHeight: this.getMaxListHeight(),
            selectionMode: "one",
            quickSelection: true
          });

          control.addListener("changeSelection", this._onListChangeSelection, this);
          control.addListener("mousedown", this._onListMouseDown, this);
          break;

        case "popup":
          control = new qx.ui.popup.Popup(new qx.ui.layout.VBox);
          control.setAutoHide(false);
          control.setKeepActive(true);
          control.addListener("click", this.close, this);
          control.add(this.getChildControl("list"));

          control.addListener("changeVisibility", this._onPopupChangeVisibility, this);
          break;
      }

      return control || this.base(arguments, id);
    },



    /*
    ---------------------------------------------------------------------------
      APPLY ROUTINES
    ---------------------------------------------------------------------------
    */

    // property apply
    _applyMaxListHeight : function(value, old) {
      this.getChildControl("list").setMaxHeight(value);
    },



    /*
    ---------------------------------------------------------------------------
      PUBLIC METHODS
    ---------------------------------------------------------------------------
    */

    /**
     * Returns the list widget.
     * @return {qx.ui.form.List} the list
     */
    getChildrenContainer : function() {
      return this.getChildControl("list");
    },



    /*
    ---------------------------------------------------------------------------
      LIST STUFF
    ---------------------------------------------------------------------------
    */

    /**
     * Shows the list popup.
     */
    open : function()
    {
      var popup = this.getChildControl("popup");

      popup.placeToWidget(this, true);
      popup.show();
    },


    /**
     * Hides the list popup.
     */
    close : function() {
      this.getChildControl("popup").hide();
    },


    /**
     * Toggles the popup's visibility.
     */
    toggle : function()
    {
      var isListOpen = this.getChildControl("popup").isVisible();
      if (isListOpen) {
        this.close();
      } else {
        this.open();
      }
    },


    /*
    ---------------------------------------------------------------------------
      FORMAT HANDLING
    ---------------------------------------------------------------------------
    */


    /**
     * Return the formatted label text from the <code>ListItem</code>.
     * The formatter removes all HTML tags and converts all HTML entities
     * to string characters when the rich property is <code>true</code>.
     *
     * @param item {ListItem} The list item to format.
     * @return {String} The formatted text.
     */
    _defaultFormat : function(item)
    {
      var valueLabel = item ? item.getLabel() : "";
      var rich = item ? item.getRich() : false;

      if (rich) {
        valueLabel = valueLabel.replace(/<[^>]+?>/g, "");
        valueLabel = qx.bom.String.unescape(valueLabel);
      }

      return valueLabel;
    },


    /*
    ---------------------------------------------------------------------------
      EVENT LISTENERS
    ---------------------------------------------------------------------------
    */

    /**
     * Handler for the blur event of the current widget.
     *
     * @param e {qx.event.type.Focus} The blur event.
     */
    _onBlur : function(e)
    {
      this.close();
    },


    /**
     * Reacts on special keys and forwards other key events to the list widget.
     *
     * @param e {qx.event.type.KeySequence} Keypress event
     */
    _onKeyPress : function(e)
    {
      // get the key identifier
      var identifier = e.getKeyIdentifier();
      var listPopup = this.getChildControl("popup");

      // disabled pageUp and pageDown keys
      if (listPopup.isHidden() && (identifier == "PageDown" || identifier == "PageUp")) {
        e.stopPropagation();
      }

      // hide the list always on escape
      else if (!listPopup.isHidden() && identifier == "Escape")
      {
        this.close();
        e.stop();
      }

      // forward the rest of the events to the list
      else
      {
        this.getChildControl("list").handleKeyPress(e);
      }
    },


    /**
     * Close the pop-up if the mousewheel event isn't on the pup-up window.
     *
     * @param e {qx.event.type.Mouse} Mousewheel event.
     * @deprecated {3.5} The widget does not need a mousewheel handler anymore.
     */
    _onMousewheel : function(e) {},


    /**
     * Updates list minimum size.
     *
     * @param e {qx.event.type.Data} Data event
     */
    _onResize : function(e){
      this.getChildControl("popup").setMinWidth(e.getData().width);
    },


    /**
     * Syncs the own property from the list change
     *
     * @param e {qx.event.type.Data} Data Event
     */
    _onListChangeSelection : function(e) {
      throw new Error("Abstract method: _onListChangeSelection()");
    },


    /**
     * Redirects mousedown event from the list to this widget.
     *
     * @param e {qx.event.type.Mouse} Mouse Event
     */
    _onListMouseDown : function(e) {
      throw new Error("Abstract method: _onListMouseDown()");
    },


    /**
     * Redirects changeVisibility event from the list to this widget.
     *
     * @param e {qx.event.type.Data} Property change event
     */
    _onPopupChangeVisibility : function(e) {
      e.getData() == "visible" ? this.addState("popupOpen") : this.removeState("popupOpen");
    }
  },

  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    // @deprecated {3.5}
    var root = qx.core.Init.getApplication().getRoot();
    if (root) {
      root.removeListener("mousewheel", this._onMousewheel, this, true);
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)

************************************************************************ */

/**
 * Generic selection manager to bring rich desktop like selection behavior
 * to widgets and low-level interactive controls.
 *
 * The selection handling supports both Shift and Ctrl/Meta modifies like
 * known from native applications.
 */
qx.Class.define("qx.ui.core.selection.Abstract",
{
  type : "abstract",
  extend : qx.core.Object,



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    // {Map} Internal selection storage
    this.__selection = {};
  },




  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events :
  {
    /** Fires after the selection was modified. Contains the selection under the data property. */
    "changeSelection" : "qx.event.type.Data"
  },





  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /**
     * Selects the selection mode to use.
     *
     * * single: One or no element is selected
     * * multi: Multi items could be selected. Also allows empty selections.
     * * additive: Easy Web-2.0 selection mode. Allows multiple selections without modifier keys.
     * * one: If possible always exactly one item is selected
     */
    mode :
    {
      check : [ "single", "multi", "additive", "one" ],
      init : "single",
      apply : "_applyMode"
    },


    /**
     * Enable drag selection (multi selection of items through
     * dragging the mouse in pressed states).
     *
     * Only possible for the modes <code>multi</code> and <code>additive</code>
     */
    drag :
    {
      check : "Boolean",
      init : false
    },


    /**
     * Enable quick selection mode, where no click is needed to change the selection.
     *
     * Only possible for the modes <code>single</code> and <code>one</code>.
     */
    quick :
    {
      check : "Boolean",
      init : false
    }
  },





  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __scrollStepX : 0,
    __scrollStepY : 0,
    __scrollTimer : null,
    __frameScroll : null,
    __lastRelX : null,
    __lastRelY : null,
    __frameLocation : null,
    __dragStartX : null,
    __dragStartY : null,
    __inCapture : null,
    __mouseX : null,
    __mouseY : null,
    __moveDirectionX : null,
    __moveDirectionY : null,
    __selectionModified : null,
    __selectionContext : null,
    __leadItem : null,
    __selection : null,
    __anchorItem : null,
    __mouseDownOnSelected : null,

    // A flag that signals an user interaction, which means the selection change
    // was triggered by mouse or keyboard [BUG #3344]
    _userInteraction : false,

    __oldScrollTop : null,

    /*
    ---------------------------------------------------------------------------
      USER APIS
    ---------------------------------------------------------------------------
    */

    /**
     * Returns the selection context. One of <code>click</code>,
     * <code>quick</code>, <code>drag</code> or <code>key</code> or
     * <code>null</code>.
     *
     * @return {String} One of <code>click</code>, <code>quick</code>,
     *    <code>drag</code> or <code>key</code> or <code>null</code>
     */
    getSelectionContext : function() {
      return this.__selectionContext;
    },


    /**
     * Selects all items of the managed object.
     *
     */
    selectAll : function()
    {
      var mode = this.getMode();
      if (mode == "single" || mode == "one") {
        throw new Error("Can not select all items in selection mode: " + mode);
      }

      this._selectAllItems();
      this._fireChange();
    },


    /**
     * Selects the given item. Replaces current selection
     * completely with the new item.
     *
     * Use {@link #addItem} instead if you want to add new
     * items to an existing selection.
     *
     * @param item {Object} Any valid item
     */
    selectItem : function(item)
    {
      this._setSelectedItem(item);

      var mode = this.getMode();
      if (mode !== "single" && mode !== "one")
      {
        this._setLeadItem(item);
        this._setAnchorItem(item);
      }

      this._scrollItemIntoView(item);
      this._fireChange();
    },


    /**
     * Adds the given item to the existing selection.
     *
     * Use {@link #selectItem} instead if you want to replace
     * the current selection.
     *
     * @param item {Object} Any valid item
     */
    addItem : function(item)
    {
      var mode = this.getMode();
      if (mode === "single" || mode === "one") {
        this._setSelectedItem(item);
      }
      else
      {
        if (this._getAnchorItem() == null) {
          this._setAnchorItem(item);
        }

        this._setLeadItem(item);
        this._addToSelection(item);
      }

      this._scrollItemIntoView(item);
      this._fireChange();
    },


    /**
     * Removes the given item from the selection.
     *
     * Use {@link #clearSelection} when you want to clear
     * the whole selection at once.
     *
     * @param item {Object} Any valid item
     */
    removeItem : function(item)
    {
      this._removeFromSelection(item);

      if (this.getMode() === "one" && this.isSelectionEmpty())
      {
        var selected = this._applyDefaultSelection();

        // Do not fire any event in this case.
        if (selected == item) {
          return;
        }
      }

      if (this.getLeadItem() == item) {
        this._setLeadItem(null);
      }

      if (this._getAnchorItem() == item) {
        this._setAnchorItem(null);
      }

      this._fireChange();
    },


    /**
     * Selects an item range between two given items.
     *
     * @param begin {Object} Item to start with
     * @param end {Object} Item to end at
     */
    selectItemRange : function(begin, end)
    {
      var mode = this.getMode();
      if (mode == "single" || mode == "one") {
        throw new Error("Can not select multiple items in selection mode: " + mode);
      }

      this._selectItemRange(begin, end);

      this._setAnchorItem(begin);

      this._setLeadItem(end);
      this._scrollItemIntoView(end);

      this._fireChange();
    },


    /**
     * Clears the whole selection at once. Also
     * resets the lead and anchor items and their
     * styles.
     *
     */
    clearSelection : function()
    {
      if (this.getMode() == "one")
      {
        var selected = this._applyDefaultSelection(true);
        if (selected != null) {
          return;
        }
      }

      this._clearSelection();
      this._setLeadItem(null);
      this._setAnchorItem(null);

      this._fireChange();
    },


    /**
     * Replaces current selection with given array of items.
     *
     * Please note that in single selection scenarios it is more
     * efficient to directly use {@link #selectItem}.
     *
     * @param items {Array} Items to select
     */
    replaceSelection : function(items)
    {
      var mode = this.getMode();
      if (mode == "one" || mode === "single")
      {
        if (items.length > 1)   {
          throw new Error("Could not select more than one items in mode: " + mode + "!");
        }

        if (items.length == 1) {
          this.selectItem(items[0]);
        } else {
          this.clearSelection();
        }
        return;
      }
      else
      {
        this._replaceMultiSelection(items);
      }
    },


    /**
     * Get the selected item. This method does only work in <code>single</code>
     * selection mode.
     *
     * @return {Object} The selected item.
     */
    getSelectedItem : function()
    {
      var mode = this.getMode();
      if (mode === "single" || mode === "one")
      {
        var result = this._getSelectedItem();
        return result != undefined ? result : null;
      }

      throw new Error("The method getSelectedItem() is only supported in 'single' and 'one' selection mode!");
    },


    /**
     * Returns an array of currently selected items.
     *
     * Note: The result is only a set of selected items, so the order can
     * differ from the sequence in which the items were added.
     *
     * @return {Object[]} List of items.
     */
    getSelection : function() {
      return qx.lang.Object.getValues(this.__selection);
    },


    /**
     * Returns the selection sorted by the index in the
     * container of the selection (the assigned widget)
     *
     * @return {Object[]} Sorted list of items
     */
    getSortedSelection : function()
    {
      var children = this.getSelectables();
      var sel = qx.lang.Object.getValues(this.__selection);

      sel.sort(function(a, b) {
        return children.indexOf(a) - children.indexOf(b);
      });

      return sel;
    },


    /**
     * Detects whether the given item is currently selected.
     *
     * @param item {var} Any valid selectable item
     * @return {Boolean} Whether the item is selected
     */
    isItemSelected : function(item)
    {
      var hash = this._selectableToHashCode(item);
      return this.__selection[hash] !== undefined;
    },


    /**
     * Whether the selection is empty
     *
     * @return {Boolean} Whether the selection is empty
     */
    isSelectionEmpty : function() {
      return qx.lang.Object.isEmpty(this.__selection);
    },


    /**
     * Invert the selection. Select the non selected and deselect the selected.
     */
    invertSelection: function() {
      var mode = this.getMode();
      if (mode === "single" || mode === "one") {
        throw new Error("The method invertSelection() is only supported in 'multi' and 'additive' selection mode!");
      }

      var selectables = this.getSelectables();
      for (var i = 0; i < selectables.length; i++)
      {
        this._toggleInSelection(selectables[i]);
      }

      this._fireChange();
    },



    /*
    ---------------------------------------------------------------------------
      LEAD/ANCHOR SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * Sets the lead item. Generally the item which was last modified
     * by the user (clicked on etc.)
     *
     * @param value {Object} Any valid item or <code>null</code>
     */
    _setLeadItem : function(value)
    {
      var old = this.__leadItem;

      if (old !== null) {
        this._styleSelectable(old, "lead", false);
      }

      if (value !== null) {
        this._styleSelectable(value, "lead", true);
      }

      this.__leadItem = value;
    },


    /**
     * Returns the current lead item. Generally the item which was last modified
     * by the user (clicked on etc.)
     *
     * @return {Object} The lead item or <code>null</code>
     */
    getLeadItem : function() {
      return this.__leadItem !== null ? this.__leadItem : null;
    },


    /**
     * Sets the anchor item. This is the item which is the starting
     * point for all range selections. Normally this is the item which was
     * clicked on the last time without any modifier keys pressed.
     *
     * @param value {Object} Any valid item or <code>null</code>
     */
    _setAnchorItem : function(value)
    {
      var old = this.__anchorItem;

      if (old != null) {
        this._styleSelectable(old, "anchor", false);
      }

      if (value != null) {
        this._styleSelectable(value, "anchor", true);
      }

      this.__anchorItem = value;
    },


    /**
     * Returns the current anchor item. This is the item which is the starting
     * point for all range selections. Normally this is the item which was
     * clicked on the last time without any modifier keys pressed.
     *
     * @return {Object} The anchor item or <code>null</code>
     */
    _getAnchorItem : function() {
      return this.__anchorItem !== null ? this.__anchorItem : null;
    },





    /*
    ---------------------------------------------------------------------------
      BASIC SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * Whether the given item is selectable.
     *
     * @param item {var} Any item
     * @return {Boolean} <code>true</code> when the item is selectable
     */
    _isSelectable : function(item) {
      throw new Error("Abstract method call: _isSelectable()");
    },


    /**
     * Finds the selectable instance from a mouse event
     *
     * @param event {qx.event.type.Mouse} The mouse event
     * @return {Object|null} The resulting selectable
     */
    _getSelectableFromMouseEvent : function(event)
    {
      var target = event.getTarget();
      // check for target (may be null when leaving the viewport) [BUG #4378]
      if (target && this._isSelectable(target)) {
        return target;
      }
      return null;
    },


    /**
     * Returns an unique hashcode for the given item.
     *
     * @param item {var} Any item
     * @return {String} A valid hashcode
     */
    _selectableToHashCode : function(item) {
      throw new Error("Abstract method call: _selectableToHashCode()");
    },


    /**
     * Updates the style (appearance) of the given item.
     *
     * @param item {var} Item to modify
     * @param type {String} Any of <code>selected</code>, <code>anchor</code> or <code>lead</code>
     * @param enabled {Boolean} Whether the given style should be added or removed.
     */
    _styleSelectable : function(item, type, enabled) {
      throw new Error("Abstract method call: _styleSelectable()");
    },


    /**
     * Enables capturing of the container.
     *
     */
    _capture : function() {
      throw new Error("Abstract method call: _capture()");
    },


    /**
     * Releases capturing of the container
     *
     */
    _releaseCapture : function() {
      throw new Error("Abstract method call: _releaseCapture()");
    },






    /*
    ---------------------------------------------------------------------------
      DIMENSION AND LOCATION
    ---------------------------------------------------------------------------
    */

    /**
     * Returns the location of the container
     *
     * @return {Map} Map with the keys <code>top</code>, <code>right</code>,
     *    <code>bottom</code> and <code>left</code>.
     */
    _getLocation : function() {
      throw new Error("Abstract method call: _getLocation()");
    },


    /**
     * Returns the dimension of the container (available scrolling space).
     *
     * @return {Map} Map with the keys <code>width</code> and <code>height</code>.
     */
    _getDimension : function() {
      throw new Error("Abstract method call: _getDimension()");
    },


    /**
     * Returns the relative (to the container) horizontal location of the given item.
     *
     * @param item {var} Any item
     * @return {Map} A map with the keys <code>left</code> and <code>right</code>.
     */
    _getSelectableLocationX : function(item) {
      throw new Error("Abstract method call: _getSelectableLocationX()");
    },


    /**
     * Returns the relative (to the container) horizontal location of the given item.
     *
     * @param item {var} Any item
     * @return {Map} A map with the keys <code>top</code> and <code>bottom</code>.
     */
    _getSelectableLocationY : function(item) {
      throw new Error("Abstract method call: _getSelectableLocationY()");
    },






    /*
    ---------------------------------------------------------------------------
      SCROLL SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * Returns the scroll position of the container.
     *
     * @return {Map} Map with the keys <code>left</code> and <code>top</code>.
     */
    _getScroll : function() {
      throw new Error("Abstract method call: _getScroll()");
    },


    /**
     * Scrolls by the given offset
     *
     * @param xoff {Integer} Horizontal offset to scroll by
     * @param yoff {Integer} Vertical offset to scroll by
     */
    _scrollBy : function(xoff, yoff) {
      throw new Error("Abstract method call: _scrollBy()");
    },


    /**
     * Scrolls the given item into the view (make it visible)
     *
     * @param item {var} Any item
     */
    _scrollItemIntoView : function(item) {
      throw new Error("Abstract method call: _scrollItemIntoView()");
    },






    /*
    ---------------------------------------------------------------------------
      QUERY SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * Returns all selectable items of the container.
     *
     * @param all {Boolean} true for all selectables, false for the
      *   selectables the user can interactively select
     * @return {Array} A list of items
     */
    getSelectables : function(all) {
      throw new Error("Abstract method call: getSelectables()");
    },


    /**
     * Returns all selectable items between the two given items.
     *
     * The items could be given in any order.
     *
     * @param item1 {var} First item
     * @param item2 {var} Second item
     * @return {Array} List of items
     */
    _getSelectableRange : function(item1, item2) {
      throw new Error("Abstract method call: _getSelectableRange()");
    },


    /**
     * Returns the first selectable item.
     *
     * @return {var} The first selectable item
     */
    _getFirstSelectable : function() {
      throw new Error("Abstract method call: _getFirstSelectable()");
    },


    /**
     * Returns the last selectable item.
     *
     * @return {var} The last selectable item
     */
    _getLastSelectable : function() {
      throw new Error("Abstract method call: _getLastSelectable()");
    },


    /**
     * Returns a selectable item which is related to the given
     * <code>item</code> through the value of <code>relation</code>.
     *
     * @param item {var} Any item
     * @param relation {String} A valid relation: <code>above</code>,
     *    <code>right</code>, <code>under</code> or <code>left</code>
     * @return {var} The related item
     */
    _getRelatedSelectable : function(item, relation) {
      throw new Error("Abstract method call: _getRelatedSelectable()");
    },


    /**
     * Returns the item which should be selected on pageUp/pageDown.
     *
     * May also scroll to the needed position.
     *
     * @param lead {var} The current lead item
     * @param up {Boolean?false} Which page key was pressed:
     *   <code>up</code> or <code>down</code>.
     */
    _getPage : function(lead, up) {
      throw new Error("Abstract method call: _getPage()");
    },




    /*
    ---------------------------------------------------------------------------
      PROPERTY APPLY ROUTINES
    ---------------------------------------------------------------------------
    */

    // property apply
    _applyMode : function(value, old)
    {
      this._setLeadItem(null);
      this._setAnchorItem(null);

      this._clearSelection();

      // Mode "one" requires one selected item
      if (value === "one") {
        this._applyDefaultSelection(true);
      }

      this._fireChange();
    },






    /*
    ---------------------------------------------------------------------------
      MOUSE SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * This method should be connected to the <code>mouseover</code> event
     * of the managed object.
     *
     * @param event {qx.event.type.Mouse} A valid mouse event
     */
    handleMouseOver : function(event)
    {
      // All browsers (except Opera) fire a native "mouseover" event when a scroll appears
      // by keyboard interaction. We have to ignore the event to avoid a selection for
      // "mouseover" (quick selection). For more details see [BUG #4225]
      if(this.__oldScrollTop != null &&
         this.__oldScrollTop != this._getScroll().top)
      {
        this.__oldScrollTop = null;
        return;
      }

      // this is a method invoked by an user interaction, so be careful to
      // set / clear the mark this._userInteraction [BUG #3344]
      this._userInteraction = true;

      if (!this.getQuick()) {
        this._userInteraction = false;
        return;
      }

      var mode = this.getMode();
      if (mode !== "one" && mode !== "single") {
        this._userInteraction = false;
        return;
      }

      var item = this._getSelectableFromMouseEvent(event);
      if (item === null) {
        this._userInteraction = false;
        return;
      }

      this._setSelectedItem(item);

      // Be sure that item is in view
      // This does not feel good when mouseover is used
      // this._scrollItemIntoView(item);

      // Fire change event as needed
      this._fireChange("quick");

      this._userInteraction = false;
    },


    /**
     * This method should be connected to the <code>mousedown</code> event
     * of the managed object.
     *
     * @param event {qx.event.type.Mouse} A valid mouse event
     */
    handleMouseDown : function(event)
    {
      // this is a method invoked by an user interaction, so be careful to
      // set / clear the mark this._userInteraction [BUG #3344]
      this._userInteraction = true;

      var item = this._getSelectableFromMouseEvent(event);
      if (item === null) {
        this._userInteraction = false;
        return;
      }

      // Read in keyboard modifiers
      var isCtrlPressed = event.isCtrlPressed() ||
        (qx.core.Environment.get("os.name") == "osx" && event.isMetaPressed());
      var isShiftPressed = event.isShiftPressed();

      // Clicking on selected items deselect on mouseup, not on mousedown
      if (this.isItemSelected(item) && !isShiftPressed && !isCtrlPressed && !this.getDrag())
      {
        this.__mouseDownOnSelected = item;
        this._userInteraction = false;
        return;
      }
      else
      {
        this.__mouseDownOnSelected = null;
      }

      // Be sure that item is in view
      this._scrollItemIntoView(item);

      // Action depends on selected mode
      switch(this.getMode())
      {
        case "single":
        case "one":
          this._setSelectedItem(item);
          break;

        case "additive":
          this._setLeadItem(item);
          this._setAnchorItem(item);
          this._toggleInSelection(item);
          break;

        case "multi":
          // Update lead item
          this._setLeadItem(item);

          // Create/Update range selection
          if (isShiftPressed)
          {
            var anchor = this._getAnchorItem();
            if (anchor === null)
            {
              anchor = this._getFirstSelectable();
              this._setAnchorItem(anchor);
            }

            this._selectItemRange(anchor, item, isCtrlPressed);
          }

          // Toggle in selection
          else if (isCtrlPressed)
          {
            this._setAnchorItem(item);
            this._toggleInSelection(item);
          }

          // Replace current selection
          else
          {
            this._setAnchorItem(item);
            this._setSelectedItem(item);
          }

          break;
      }


      // Drag selection
      var mode = this.getMode();
      if (
        this.getDrag() &&
        mode !== "single" &&
        mode !== "one" &&
        !isShiftPressed &&
        !isCtrlPressed
      )
      {
        // Cache location/scroll data
        this.__frameLocation = this._getLocation();
        this.__frameScroll = this._getScroll();

        // Store position at start
        this.__dragStartX = event.getDocumentLeft() + this.__frameScroll.left;
        this.__dragStartY = event.getDocumentTop() + this.__frameScroll.top;

        // Switch to capture mode
        this.__inCapture = true;
        this._capture();
      }


      // Fire change event as needed
      this._fireChange("click");

      this._userInteraction = false;
    },


    /**
     * This method should be connected to the <code>mouseup</code> event
     * of the managed object.
     *
     * @param event {qx.event.type.Mouse} A valid mouse event
     */
    handleMouseUp : function(event)
    {
      // this is a method invoked by an user interaction, so be careful to
      // set / clear the mark this._userInteraction [BUG #3344]
      this._userInteraction = true;

      // Read in keyboard modifiers
      var isCtrlPressed = event.isCtrlPressed() ||
        (qx.core.Environment.get("os.name") == "osx" && event.isMetaPressed());
      var isShiftPressed = event.isShiftPressed();

      if (!isCtrlPressed && !isShiftPressed && this.__mouseDownOnSelected != null)
      {
        var item = this._getSelectableFromMouseEvent(event);
        if (item === null || !this.isItemSelected(item)) {
          this._userInteraction = false;
          return;
        }

        var mode = this.getMode();
        if (mode === "additive")
        {
          // Remove item from selection
          this._removeFromSelection(item);
        }
        else
        {
          // Replace selection
          this._setSelectedItem(item);

          if (this.getMode() === "multi")
          {
            this._setLeadItem(item);
            this._setAnchorItem(item);
          }
        }
        this._userInteraction = false;
      }

      // Cleanup operation
      this._cleanup();
    },


    /**
     * This method should be connected to the <code>losecapture</code> event
     * of the managed object.
     *
     * @param event {qx.event.type.Mouse} A valid mouse event
     */
    handleLoseCapture : function(event) {
      this._cleanup();
    },


    /**
     * This method should be connected to the <code>mousemove</code> event
     * of the managed object.
     *
     * @param event {qx.event.type.Mouse} A valid mouse event
     */
    handleMouseMove : function(event)
    {
      // Only relevant when capturing is enabled
      if (!this.__inCapture) {
        return;
      }


      // Update mouse position cache
      this.__mouseX = event.getDocumentLeft();
      this.__mouseY = event.getDocumentTop();

      // this is a method invoked by an user interaction, so be careful to
      // set / clear the mark this._userInteraction [BUG #3344]
      this._userInteraction = true;

      // Detect move directions
      var dragX = this.__mouseX + this.__frameScroll.left;
      if (dragX > this.__dragStartX) {
        this.__moveDirectionX = 1;
      } else if (dragX < this.__dragStartX) {
        this.__moveDirectionX = -1;
      } else {
        this.__moveDirectionX = 0;
      }

      var dragY = this.__mouseY + this.__frameScroll.top;
      if (dragY > this.__dragStartY) {
        this.__moveDirectionY = 1;
      } else if (dragY < this.__dragStartY) {
        this.__moveDirectionY = -1;
      } else {
        this.__moveDirectionY = 0;
      }


      // Update scroll steps
      var location = this.__frameLocation;

      if (this.__mouseX < location.left) {
        this.__scrollStepX = this.__mouseX - location.left;
      } else if (this.__mouseX > location.right) {
        this.__scrollStepX = this.__mouseX - location.right;
      } else {
        this.__scrollStepX = 0;
      }

      if (this.__mouseY < location.top) {
        this.__scrollStepY = this.__mouseY - location.top;
      } else if (this.__mouseY > location.bottom) {
        this.__scrollStepY = this.__mouseY - location.bottom;
      } else {
        this.__scrollStepY = 0;
      }


      // Dynamically create required timer instance
      if (!this.__scrollTimer)
      {
        this.__scrollTimer = new qx.event.Timer(100);
        this.__scrollTimer.addListener("interval", this._onInterval, this);
      }


      // Start interval
      this.__scrollTimer.start();


      // Auto select based on new cursor position
      this._autoSelect();

      event.stopPropagation();
      this._userInteraction = false;
    },


    /**
     * This method should be connected to the <code>addItem</code> event
     * of the managed object.
     *
     * @param e {qx.event.type.Data} The event object
     */
    handleAddItem : function(e)
    {
      var item = e.getData();
      if (this.getMode() === "one" && this.isSelectionEmpty()) {
        this.addItem(item);
      }
    },


    /**
     * This method should be connected to the <code>removeItem</code> event
     * of the managed object.
     *
     * @param e {qx.event.type.Data} The event object
     */
    handleRemoveItem : function(e) {
      this.removeItem(e.getData());
    },




    /*
    ---------------------------------------------------------------------------
      MOUSE SUPPORT INTERNALS
    ---------------------------------------------------------------------------
    */

    /**
     * Stops all timers, release capture etc. to cleanup drag selection
     */
    _cleanup : function()
    {
      if (!this.getDrag() && this.__inCapture) {
        return;
      }

      // Fire change event if needed
      if (this.__selectionModified) {
        this._fireChange("click");
      }

      // Remove flags
      delete this.__inCapture;
      delete this.__lastRelX;
      delete this.__lastRelY;

      // Stop capturing
      this._releaseCapture();

      // Stop timer
      if (this.__scrollTimer) {
        this.__scrollTimer.stop();
      }
    },


    /**
     * Event listener for timer used by drag selection
     *
     * @param e {qx.event.type.Event} Timer event
     */
    _onInterval : function(e)
    {
      // Scroll by defined block size
      this._scrollBy(this.__scrollStepX, this.__scrollStepY);

      // Update scroll cache
      this.__frameScroll = this._getScroll();

      // Auto select based on new scroll position and cursor
      this._autoSelect();
    },


    /**
     * Automatically selects items based on the mouse movement during a drag selection
     */
    _autoSelect : function()
    {
      var inner = this._getDimension();

      // Get current relative Y position and compare it with previous one
      var relX = Math.max(0, Math.min(this.__mouseX - this.__frameLocation.left, inner.width)) + this.__frameScroll.left;
      var relY = Math.max(0, Math.min(this.__mouseY - this.__frameLocation.top, inner.height)) + this.__frameScroll.top;

      // Compare old and new relative coordinates (for performance reasons)
      if (this.__lastRelX === relX && this.__lastRelY === relY) {
        return;
      }
      this.__lastRelX = relX;
      this.__lastRelY = relY;

      // Cache anchor
      var anchor = this._getAnchorItem();
      var lead = anchor;


      // Process X-coordinate
      var moveX = this.__moveDirectionX;
      var nextX, locationX;

      while (moveX !== 0)
      {
        // Find next item to process depending on current scroll direction
        nextX = moveX > 0 ?
          this._getRelatedSelectable(lead, "right") :
          this._getRelatedSelectable(lead, "left");

        // May be null (e.g. first/last item)
        if (nextX !== null)
        {
          locationX = this._getSelectableLocationX(nextX);

          // Continue when the item is in the visible area
          if (
            (moveX > 0 && locationX.left <= relX) ||
            (moveX < 0 && locationX.right >= relX)
          )
          {
            lead = nextX;
            continue;
          }
        }

        // Otherwise break
        break;
      }


      // Process Y-coordinate
      var moveY = this.__moveDirectionY;
      var nextY, locationY;

      while (moveY !== 0)
      {
        // Find next item to process depending on current scroll direction
        nextY = moveY > 0 ?
          this._getRelatedSelectable(lead, "under") :
          this._getRelatedSelectable(lead, "above");

        // May be null (e.g. first/last item)
        if (nextY !== null)
        {
          locationY = this._getSelectableLocationY(nextY);

          // Continue when the item is in the visible area
          if (
            (moveY > 0 && locationY.top <= relY) ||
            (moveY < 0 && locationY.bottom >= relY)
          )
          {
            lead = nextY;
            continue;
          }
        }

        // Otherwise break
        break;
      }


      // Differenciate between the two supported modes
      var mode = this.getMode();
      if (mode === "multi")
      {
        // Replace current selection with new range
        this._selectItemRange(anchor, lead);
      }
      else if (mode === "additive")
      {
        // Behavior depends on the fact whether the
        // anchor item is selected or not
        if (this.isItemSelected(anchor)) {
          this._selectItemRange(anchor, lead, true);
        } else {
          this._deselectItemRange(anchor, lead);
        }

        // Improve performance. This mode does not rely
        // on full ranges as it always extend the old
        // selection/deselection.
        this._setAnchorItem(lead);
      }


      // Fire change event as needed
      this._fireChange("drag");
    },






    /*
    ---------------------------------------------------------------------------
      KEYBOARD SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * @type {Map} All supported navigation keys
     *
     * @lint ignoreReferenceField(__navigationKeys)
     */
    __navigationKeys :
    {
      Home : 1,
      Down : 1 ,
      Right : 1,
      PageDown : 1,
      End : 1,
      Up : 1,
      Left : 1,
      PageUp : 1
    },


    /**
     * This method should be connected to the <code>keypress</code> event
     * of the managed object.
     *
     * @param event {qx.event.type.KeySequence} A valid key sequence event
     */
    handleKeyPress : function(event)
    {
      // this is a method invoked by an user interaction, so be careful to
      // set / clear the mark this._userInteraction [BUG #3344]
      this._userInteraction = true;

      var current, next;
      var key = event.getKeyIdentifier();
      var mode = this.getMode();

      // Support both control keys on Mac
      var isCtrlPressed = event.isCtrlPressed() ||
        (qx.core.Environment.get("os.name") == "osx" && event.isMetaPressed());
      var isShiftPressed = event.isShiftPressed();

      var consumed = false;

      if (key === "A" && isCtrlPressed)
      {
        if (mode !== "single" && mode !== "one")
        {
          this._selectAllItems();
          consumed = true;
        }
      }
      else if (key === "Escape")
      {
        if (mode !== "single" && mode !== "one")
        {
          this._clearSelection();
          consumed = true;
        }
      }
      else if (key === "Space")
      {
        var lead = this.getLeadItem();
        if (lead != null && !isShiftPressed)
        {
          if (isCtrlPressed || mode === "additive") {
            this._toggleInSelection(lead);
          } else {
            this._setSelectedItem(lead);
          }
          consumed = true;
        }
      }
      else if (this.__navigationKeys[key])
      {
        consumed = true;
        if (mode === "single" || mode == "one") {
          current = this._getSelectedItem();
        } else {
          current = this.getLeadItem();
        }

        if (current !== null)
        {
          switch(key)
          {
            case "Home":
              next = this._getFirstSelectable();
              break;

            case "End":
              next = this._getLastSelectable();
              break;

            case "Up":
              next = this._getRelatedSelectable(current, "above");
              break;

            case "Down":
              next = this._getRelatedSelectable(current, "under");
              break;

            case "Left":
              next = this._getRelatedSelectable(current, "left");
              break;

            case "Right":
              next = this._getRelatedSelectable(current, "right");
              break;

            case "PageUp":
              next = this._getPage(current, true);
              break;

            case "PageDown":
              next = this._getPage(current, false);
              break;
          }
        }
        else
        {
          switch(key)
          {
            case "Home":
            case "Down":
            case "Right":
            case "PageDown":
              next = this._getFirstSelectable();
              break;

            case "End":
            case "Up":
            case "Left":
            case "PageUp":
              next = this._getLastSelectable();
              break;
          }
        }

        // Process result
        if (next !== null)
        {
          switch(mode)
          {
            case "single":
            case "one":
              this._setSelectedItem(next);
              break;

            case "additive":
              this._setLeadItem(next);
              break;

            case "multi":
              if (isShiftPressed)
              {
                var anchor = this._getAnchorItem();
                if (anchor === null) {
                  this._setAnchorItem(anchor = this._getFirstSelectable());
                }

                this._setLeadItem(next);
                this._selectItemRange(anchor, next, isCtrlPressed);
              }
              else
              {
                this._setAnchorItem(next);
                this._setLeadItem(next);

                if (!isCtrlPressed) {
                  this._setSelectedItem(next);
                }
              }

              break;
          }

          this.__oldScrollTop = this._getScroll().top;
          this._scrollItemIntoView(next);
        }
      }


      if (consumed)
      {
        // Stop processed events
        event.stop();

        // Fire change event as needed
        this._fireChange("key");
      }
      this._userInteraction = false;
    },






    /*
    ---------------------------------------------------------------------------
      SUPPORT FOR ITEM RANGES
    ---------------------------------------------------------------------------
    */

    /**
     * Adds all items to the selection
     */
    _selectAllItems : function()
    {
      var range = this.getSelectables();
      for (var i=0, l=range.length; i<l; i++) {
        this._addToSelection(range[i]);
      }
    },


    /**
     * Clears current selection
     */
    _clearSelection : function()
    {
      var selection = this.__selection;
      for (var hash in selection) {
        this._removeFromSelection(selection[hash]);
      }
      this.__selection = {};
    },


    /**
     * Select a range from <code>item1</code> to <code>item2</code>.
     *
     * @param item1 {Object} Start with this item
     * @param item2 {Object} End with this item
     * @param extend {Boolean?false} Whether the current
     *    selection should be replaced or extended.
     */
    _selectItemRange : function(item1, item2, extend)
    {
      var range = this._getSelectableRange(item1, item2);

      // Remove items which are not in the detected range
      if (!extend)
      {
        var selected = this.__selection;
        var mapped = this.__rangeToMap(range);

        for (var hash in selected)
        {
          if (!mapped[hash]) {
            this._removeFromSelection(selected[hash]);
          }
        }
      }

      // Add new items to the selection
      for (var i=0, l=range.length; i<l; i++) {
        this._addToSelection(range[i]);
      }
    },


    /**
     * Deselect all items between <code>item1</code> and <code>item2</code>.
     *
     * @param item1 {Object} Start with this item
     * @param item2 {Object} End with this item
     */
    _deselectItemRange : function(item1, item2)
    {
      var range = this._getSelectableRange(item1, item2);
      for (var i=0, l=range.length; i<l; i++) {
        this._removeFromSelection(range[i]);
      }
    },


    /**
     * Internal method to convert a range to a map of hash
     * codes for faster lookup during selection compare routines.
     *
     * @param range {Array} List of selectable items
     */
    __rangeToMap : function(range)
    {
      var mapped = {};
      var item;

      for (var i=0, l=range.length; i<l; i++)
      {
        item = range[i];
        mapped[this._selectableToHashCode(item)] = item;
      }

      return mapped;
    },






    /*
    ---------------------------------------------------------------------------
      SINGLE ITEM QUERY AND MODIFICATION
    ---------------------------------------------------------------------------
    */

    /**
     * Returns the first selected item. Only makes sense
     * when using manager in single selection mode.
     *
     * @return {var} The selected item (or <code>null</code>)
     */
    _getSelectedItem : function()
    {
      for (var hash in this.__selection) {
        return this.__selection[hash];
      }

      return null;
    },


    /**
     * Replace current selection with given item.
     *
     * @param item {var} Any valid selectable item
     */
    _setSelectedItem : function(item)
    {
      if (this._isSelectable(item))
      {
        // If already selected try to find out if this is the only item
        var current = this.__selection;
        var hash = this._selectableToHashCode(item);

        if (!current[hash] || (current.length >= 2))
        {
          this._clearSelection();
          this._addToSelection(item);
        }
      }
    },







    /*
    ---------------------------------------------------------------------------
      MODIFY ITEM SELECTION
    ---------------------------------------------------------------------------
    */

    /**
     * Adds an item to the current selection.
     *
     * @param item {Object} Any item
     */
    _addToSelection : function(item)
    {
      var hash = this._selectableToHashCode(item);

      if (this.__selection[hash] == null && this._isSelectable(item))
      {
        this.__selection[hash] = item;
        this._styleSelectable(item, "selected", true);

        this.__selectionModified = true;
      }
    },


    /**
     * Toggles the item e.g. remove it when already selected
     * or select it when currently not.
     *
     * @param item {Object} Any item
     */
    _toggleInSelection : function(item)
    {
      var hash = this._selectableToHashCode(item);

      if (this.__selection[hash] == null)
      {
        this.__selection[hash] = item;
        this._styleSelectable(item, "selected", true);
      }
      else
      {
        delete this.__selection[hash];
        this._styleSelectable(item, "selected", false);
      }

      this.__selectionModified = true;
    },


    /**
     * Removes the given item from the current selection.
     *
     * @param item {Object} Any item
     */
    _removeFromSelection : function(item)
    {
      var hash = this._selectableToHashCode(item);

      if (this.__selection[hash] != null)
      {
        delete this.__selection[hash];
        this._styleSelectable(item, "selected", false);

        this.__selectionModified = true;
      }
    },


    /**
     * Replaces current selection with items from given array.
     *
     * @param items {Array} List of items to select
     */
    _replaceMultiSelection : function(items)
    {
      var modified = false;

      // Build map from hash codes and filter non-selectables
      var selectable, hash;
      var incoming = {};
      for (var i=0, l=items.length; i<l; i++)
      {
        selectable = items[i];
        if (this._isSelectable(selectable))
        {
          hash = this._selectableToHashCode(selectable);
          incoming[hash] = selectable;
        }
      }

      // Remember last
      var first = items[0];
      var last = selectable;

      // Clear old entries from map
      var current = this.__selection;
      for (var hash in current)
      {
        if (incoming[hash])
        {
          // Reduce map to make next loop faster
          delete incoming[hash];
        }
        else
        {
          // update internal map
          selectable = current[hash];
          delete current[hash];

          // apply styling
          this._styleSelectable(selectable, "selected", false);

          // remember that the selection has been modified
          modified = true;
        }
      }

      // Add remaining selectables to selection
      for (var hash in incoming)
      {
        // update internal map
        selectable = current[hash] = incoming[hash];

        // apply styling
        this._styleSelectable(selectable, "selected", true);

        // remember that the selection has been modified
        modified = true;
      }

      // Do not do anything if selection is equal to previous one
      if (!modified) {
        return false;
      }

      // Scroll last incoming item into view
      this._scrollItemIntoView(last);

      // Reset anchor and lead item
      this._setLeadItem(first);
      this._setAnchorItem(first);

      // Finally fire change event
      this.__selectionModified = true;
      this._fireChange();
    },


    /**
     * Fires the selection change event if the selection has
     * been modified.
     *
     * @param context {String} One of <code>click</code>, <code>quick</code>,
     *    <code>drag</code> or <code>key</code> or <code>null</code>
     */
    _fireChange : function(context)
    {
      if (this.__selectionModified)
      {
        // Store context
        this.__selectionContext = context || null;

        // Fire data event which contains the current selection
        this.fireDataEvent("changeSelection", this.getSelection());
        delete this.__selectionModified;
      }
    },


    /**
     * Applies the default selection. The default item is the first item.
     *
     * @param force {Boolean} Whether the default selection sould forced.
     *
     * @return {var} The selected item.
     */
    _applyDefaultSelection : function(force)
    {
      if (force === true || this.getMode() === "one" && this.isSelectionEmpty())
      {
        var first = this._getFirstSelectable();
        if (first != null) {
          this.selectItem(first);
        }
        return first;
      }
      return null;
    }
  },


  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    this._disposeObjects("__scrollTimer");
    this.__selection = this.__mouseDownOnSelected = this.__anchorItem = null;
    this.__leadItem = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)

************************************************************************ */

/**
 * A selection manager, which handles the selection in widgets.
 */
qx.Class.define("qx.ui.core.selection.Widget",
{
  extend : qx.ui.core.selection.Abstract,



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param widget {qx.ui.core.Widget} The widget to connect to
   */
  construct : function(widget)
  {
    this.base(arguments);

    this.__widget = widget;
  },





  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {

    __widget : null,

    /*
    ---------------------------------------------------------------------------
      BASIC SUPPORT
    ---------------------------------------------------------------------------
    */

    // overridden
    _isSelectable : function(item) {
      return this._isItemSelectable(item) && item.getLayoutParent() === this.__widget;
    },


    // overridden
    _selectableToHashCode : function(item) {
      return item.$$hash;
    },


    // overridden
    _styleSelectable : function(item, type, enabled) {
      enabled ? item.addState(type) : item.removeState(type);
    },


    // overridden
    _capture : function() {
      this.__widget.capture();
    },


    // overridden
    _releaseCapture : function() {
      this.__widget.releaseCapture();
    },


    /**
     * Helper to return the selectability of the item concerning the
     * user interaaction.
     *
     * @param item {qx.ui.core.Widget} The item to check.
     * @return {Boolean} true, if the item is selectable.
     */
    _isItemSelectable : function(item) {
      if (this._userInteraction) {
        return item.isVisible() && item.isEnabled();
      } else {
        return item.isVisible();
      }
    },


    /**
     * Returns the connected widget.
     * @return {qx.ui.core.Widget} The widget
     */
    _getWidget : function() {
      return this.__widget;
    },




    /*
    ---------------------------------------------------------------------------
      DIMENSION AND LOCATION
    ---------------------------------------------------------------------------
    */

    // overridden
    _getLocation : function()
    {
      var elem = this.__widget.getContentElement().getDomElement();
      return elem ? qx.bom.element.Location.get(elem) : null;
    },


    // overridden
    _getDimension : function() {
      return this.__widget.getInnerSize();
    },


    // overridden
    _getSelectableLocationX : function(item)
    {
      var computed = item.getBounds();
      if (computed)
      {
        return {
          left : computed.left,
          right : computed.left + computed.width
        };
      }
    },


    // overridden
    _getSelectableLocationY : function(item)
    {
      var computed = item.getBounds();
      if (computed)
      {
        return {
          top : computed.top,
          bottom : computed.top + computed.height
        };
      }
    },






    /*
    ---------------------------------------------------------------------------
      SCROLL SUPPORT
    ---------------------------------------------------------------------------
    */

    // overridden
    _getScroll : function()
    {
      return {
        left : 0,
        top : 0
      };
    },


    // overridden
    _scrollBy : function(xoff, yoff) {
      // empty implementation
    },


    // overridden
    _scrollItemIntoView : function(item) {
      this.__widget.scrollChildIntoView(item);
    },






    /*
    ---------------------------------------------------------------------------
      QUERY SUPPORT
    ---------------------------------------------------------------------------
    */

    // overridden
    getSelectables : function(all)
    {
      // if only the user selectables should be returned
      var oldUserInteraction = false;
      if (!all) {
        oldUserInteraction = this._userInteraction;
        this._userInteraction = true;
      }
      var children = this.__widget.getChildren();
      var result = [];
      var child;

      for (var i=0, l=children.length; i<l; i++)
      {
        child = children[i];

        if (this._isItemSelectable(child)) {
          result.push(child);
        }
      }

      // reset to the former user interaction state
      this._userInteraction = oldUserInteraction;
      return result;
    },


    // overridden
    _getSelectableRange : function(item1, item2)
    {
      // Fast path for identical items
      if (item1 === item2) {
        return [item1];
      }

      // Iterate over children and collect all items
      // between the given two (including them)
      var children = this.__widget.getChildren();
      var result = [];
      var active = false;
      var child;

      for (var i=0, l=children.length; i<l; i++)
      {
        child = children[i];

        if (child === item1 || child === item2)
        {
          if (active)
          {
            result.push(child);
            break;
          }
          else
          {
            active = true;
          }
        }

        if (active && this._isItemSelectable(child)) {
          result.push(child);
        }
      }

      return result;
    },


    // overridden
    _getFirstSelectable : function()
    {
      var children = this.__widget.getChildren();
      for (var i=0, l=children.length; i<l; i++)
      {
        if (this._isItemSelectable(children[i])) {
          return children[i];
        }
      }

      return null;
    },


    // overridden
    _getLastSelectable : function()
    {
      var children = this.__widget.getChildren();
      for (var i=children.length-1; i>0; i--)
      {
        if (this._isItemSelectable(children[i])) {
          return children[i];
        }
      }

      return null;
    },


    // overridden
    _getRelatedSelectable : function(item, relation)
    {
      var vertical = this.__widget.getOrientation() === "vertical";
      var children = this.__widget.getChildren();
      var index = children.indexOf(item);
      var sibling;

      if ((vertical && relation === "above") || (!vertical && relation === "left"))
      {
        for (var i=index-1; i>=0; i--)
        {
          sibling = children[i];
          if (this._isItemSelectable(sibling)) {
            return sibling;
          }
        }
      }
      else if ((vertical && relation === "under") || (!vertical && relation === "right"))
      {
        for (var i=index+1; i<children.length; i++)
        {
          sibling = children[i];
          if (this._isItemSelectable(sibling)) {
            return sibling;
          }
        }
      }

      return null;
    },


    // overridden
    _getPage : function(lead, up)
    {
      if (up) {
        return this._getFirstSelectable();
      } else {
        return this._getLastSelectable();
      }
    }
  },




  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function() {
    this.__widget = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)

************************************************************************ */


/**
 * A selection manager, which handles the selection in widgets extending
 * {@link qx.ui.core.scroll.AbstractScrollArea}.
 */
qx.Class.define("qx.ui.core.selection.ScrollArea",
{
  extend : qx.ui.core.selection.Widget,




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
    ---------------------------------------------------------------------------
      BASIC SUPPORT
    ---------------------------------------------------------------------------
    */

    // overridden
    _isSelectable : function(item)
    {
      return this._isItemSelectable(item) &&
        item.getLayoutParent() === this._getWidget().getChildrenContainer();
    },





    /*
    ---------------------------------------------------------------------------
      DIMENSION AND LOCATION
    ---------------------------------------------------------------------------
    */

    // overridden
    _getDimension : function() {
      return this._getWidget().getPaneSize();
    },





    /*
    ---------------------------------------------------------------------------
      SCROLL SUPPORT
    ---------------------------------------------------------------------------
    */

    // overridden
    _getScroll : function()
    {
      var widget = this._getWidget();

      return {
        left : widget.getScrollX(),
        top : widget.getScrollY()
      };
    },


    // overridden
    _scrollBy : function(xoff, yoff)
    {
      var widget = this._getWidget();

      widget.scrollByX(xoff);
      widget.scrollByY(yoff);
    },






    /*
    ---------------------------------------------------------------------------
      QUERY SUPPORT
    ---------------------------------------------------------------------------
    */

    // overridden
    _getPage : function(lead, up)
    {
      var selectables = this.getSelectables();
      var length = selectables.length;
      var start = selectables.indexOf(lead);

      // Given lead is not a selectable?!?
      if (start === -1) {
        throw new Error("Invalid lead item: " + lead);
      }

      var widget = this._getWidget();
      var scrollTop = widget.getScrollY();
      var innerHeight = widget.getInnerSize().height;
      var top, bottom, found;

      if (up)
      {
        var min = scrollTop;
        var i=start;

        // Loop required to scroll pages up dynamically
        while(1)
        {
          // Iterate through all selectables from start
          for (; i>=0; i--)
          {
            top = widget.getItemTop(selectables[i]);

            // This item is out of the visible block
            if (top < min)
            {
              // Use previous one
              found = i+1;
              break;
            }
          }

          // Nothing found. Return first item.
          if (found == null)
          {
            var first = this._getFirstSelectable();
            return first == lead ? null : first;
          }

          // Found item, but is identical to start or even before start item
          // Update min positon and try on previous page
          if (found >= start)
          {
            // Reduce min by the distance of the lead item to the visible
            // bottom edge. This is needed instead of a simple subtraction
            // of the inner height to keep the last lead visible on page key
            // presses. This is the behavior of native toolkits as well.
            min -= innerHeight + scrollTop - widget.getItemBottom(lead);
            found = null;
            continue;
          }

          // Return selectable
          return selectables[found];
        }
      }
      else
      {
        var max = innerHeight + scrollTop;
        var i=start;

        // Loop required to scroll pages down dynamically
        while(1)
        {
          // Iterate through all selectables from start
          for (; i<length; i++)
          {
            bottom = widget.getItemBottom(selectables[i]);

            // This item is out of the visible block
            if (bottom > max)
            {
              // Use previous one
              found = i-1;
              break;
            }
          }

          // Nothing found. Return last item.
          if (found == null)
          {
            var last = this._getLastSelectable();
            return last == lead ? null : last;
          }

          // Found item, but is identical to start or even before start item
          // Update max position and try on next page
          if (found <= start)
          {
            // Extend max by the distance of the lead item to the visible
            // top edge. This is needed instead of a simple addition
            // of the inner height to keep the last lead visible on page key
            // presses. This is the behavior of native toolkits as well.
            max += widget.getItemTop(lead) - scrollTop;
            found = null;
            continue;
          }

          // Return selectable
          return selectables[found];
        }
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Christian Hagendorn (chris_schmidt)

************************************************************************ */

/**
 * This mixin links all methods to manage the multi selection from the
 * internal selection manager to the widget.
 */
qx.Mixin.define("qx.ui.core.MMultiSelectionHandling",
{
  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    // Create selection manager
    var clazz = this.SELECTION_MANAGER;
    var manager = this.__manager = new clazz(this);

    // Add widget event listeners
    this.addListener("mousedown", manager.handleMouseDown, manager);
    this.addListener("mouseup", manager.handleMouseUp, manager);
    this.addListener("mouseover", manager.handleMouseOver, manager);
    this.addListener("mousemove", manager.handleMouseMove, manager);
    this.addListener("losecapture", manager.handleLoseCapture, manager);
    this.addListener("keypress", manager.handleKeyPress, manager);

    this.addListener("addItem", manager.handleAddItem, manager);
    this.addListener("removeItem", manager.handleRemoveItem, manager);

    // Add manager listeners
    manager.addListener("changeSelection", this._onSelectionChange, this);
  },


  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events :
  {
    /** Fires after the selection was modified */
    "changeSelection" : "qx.event.type.Data"
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */


  properties :
  {
    /**
     * The selection mode to use.
     *
     * For further details please have a look at:
     * {@link qx.ui.core.selection.Abstract#mode}
     */
    selectionMode :
    {
      check : [ "single", "multi", "additive", "one" ],
      init : "single",
      apply : "_applySelectionMode"
    },

    /**
     * Enable drag selection (multi selection of items through
     * dragging the mouse in pressed states).
     *
     * Only possible for the selection modes <code>multi</code> and <code>additive</code>
     */
    dragSelection :
    {
      check : "Boolean",
      init : false,
      apply : "_applyDragSelection"
    },

    /**
     * Enable quick selection mode, where no click is needed to change the selection.
     *
     * Only possible for the modes <code>single</code> and <code>one</code>.
     */
    quickSelection :
    {
      check : "Boolean",
      init : false,
      apply : "_applyQuickSelection"
    }
  },


  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */


  members :
  {
    /** @type {qx.ui.core.selection.Abstract} The selection manager */
    __manager : null,


    /*
    ---------------------------------------------------------------------------
      USER API
    ---------------------------------------------------------------------------
    */


    /**
     * Selects all items of the managed object.
     */
    selectAll : function() {
      this.__manager.selectAll();
    },


    /**
     * Detects whether the given item is currently selected.
     *
     * @param item {qx.ui.core.Widget} Any valid selectable item.
     * @return {Boolean} Whether the item is selected.
     * @throws {Error} if the item is not a child element.
     */
    isSelected : function(item) {
      if (!qx.ui.core.Widget.contains(this, item)) {
        throw new Error("Could not test if " + item +
          " is selected, because it is not a child element!");
      }

      return this.__manager.isItemSelected(item);
    },


    /**
     * Adds the given item to the existing selection.
     *
     * Use {@link #setSelection} instead if you want to replace
     * the current selection.
     *
     * @param item {qx.ui.core.Widget} Any valid item.
     * @throws {Error} if the item is not a child element.
     */
    addToSelection : function(item) {
      if (!qx.ui.core.Widget.contains(this, item)) {
        throw new Error("Could not add + " + item +
          " to selection, because it is not a child element!");
      }

      this.__manager.addItem(item);
    },


    /**
     * Removes the given item from the selection.
     *
     * Use {@link #resetSelection} when you want to clear
     * the whole selection at once.
     *
     * @param item {qx.ui.core.Widget} Any valid item
     * @throws {Error} if the item is not a child element.
     */
    removeFromSelection : function(item) {
      if (!qx.ui.core.Widget.contains(this, item)) {
        throw new Error("Could not remove " + item +
          " from selection, because it is not a child element!");
      }

      this.__manager.removeItem(item);
    },


    /**
     * Selects an item range between two given items.
     *
     * @param begin {qx.ui.core.Widget} Item to start with
     * @param end {qx.ui.core.Widget} Item to end at
     */
    selectRange : function(begin, end) {
      this.__manager.selectItemRange(begin, end);
    },


    /**
     * Clears the whole selection at once. Also
     * resets the lead and anchor items and their
     * styles.
     */
    resetSelection : function() {
      this.__manager.clearSelection();
    },


    /**
     * Replaces current selection with the given items.
     *
     * @param items {qx.ui.core.Widget[]} Items to select.
     * @throws {Error} if one of the items is not a child element and if
     *    the mode is set to <code>single</code> or <code>one</code> and
     *    the items contains more than one item.
     */
    setSelection : function(items) {
      for (var i = 0; i < items.length; i++) {
        if (!qx.ui.core.Widget.contains(this, items[i])) {
          throw new Error("Could not select " + items[i] +
            ", because it is not a child element!");
        }
      }

      if (items.length === 0) {
        this.resetSelection();
      } else {
        var currentSelection = this.getSelection();
        if (!qx.lang.Array.equals(currentSelection, items)) {
          this.__manager.replaceSelection(items);
        }
      }
    },


    /**
     * Returns an array of currently selected items.
     *
     * Note: The result is only a set of selected items, so the order can
     * differ from the sequence in which the items were added.
     *
     * @return {qx.ui.core.Widget[]} List of items.
     */
    getSelection : function() {
      return this.__manager.getSelection();
    },

    /**
     * Returns an array of currently selected items sorted
     * by their index in the container.
     *
     * @return {qx.ui.core.Widget[]} Sorted list of items
     */
    getSortedSelection : function() {
      return this.__manager.getSortedSelection();
    },

    /**
     * Whether the selection is empty
     *
     * @return {Boolean} Whether the selection is empty
     */
    isSelectionEmpty : function() {
      return this.__manager.isSelectionEmpty();
    },

    /**
     * Returns the last selection context.
     *
     * @return {String | null} One of <code>click</code>, <code>quick</code>,
     *    <code>drag</code> or <code>key</code> or <code>null</code>.
     */
    getSelectionContext : function() {
      return this.__manager.getSelectionContext();
    },

    /**
     * Returns the internal selection manager. Use this with
     * caution!
     *
     * @return {qx.ui.core.selection.Abstract} The selection manager
     */
    _getManager : function() {
      return this.__manager;
    },

    /**
     * Returns all elements which are selectable.
     *
     * @param all {Boolean} true for all selectables, false for the
     *   selectables the user can interactively select
     * @return {qx.ui.core.Widget[]} The contained items.
     */
    getSelectables: function(all) {
      return this.__manager.getSelectables(all);
    },

    /**
     * Invert the selection. Select the non selected and deselect the selected.
     */
    invertSelection: function() {
      this.__manager.invertSelection();
    },


    /**
     * Returns the current lead item. Generally the item which was last modified
     * by the user (clicked on etc.)
     *
     * @return {qx.ui.core.Widget} The lead item or <code>null</code>
     */
    _getLeadItem : function() {
      var mode = this.__manager.getMode();

      if (mode === "single" || mode === "one") {
        return this.__manager.getSelectedItem();
      } else {
        return this.__manager.getLeadItem();
      }
    },


    /*
    ---------------------------------------------------------------------------
      PROPERTY APPLY ROUTINES
    ---------------------------------------------------------------------------
    */


    // property apply
    _applySelectionMode : function(value, old) {
      this.__manager.setMode(value);
    },

    // property apply
    _applyDragSelection : function(value, old) {
      this.__manager.setDrag(value);
    },

    // property apply
    _applyQuickSelection : function(value, old) {
      this.__manager.setQuick(value);
    },


    /*
    ---------------------------------------------------------------------------
      EVENT HANDLER
    ---------------------------------------------------------------------------
    */


    /**
     * Event listener for <code>changeSelection</code> event on selection manager.
     *
     * @param e {qx.event.type.Data} Data event
     */
    _onSelectionChange : function(e) {
      this.fireDataEvent("changeSelection", e.getData());
    }
  },


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */


  destruct : function() {
    this._disposeObjects("__manager");
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2013 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Richard Sternagel (rsternagel)

************************************************************************ */

/**
 * Provides scrolling ability during drag session to the widget.
 */
qx.Mixin.define("qx.ui.core.MDragDropScrolling",
{
  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.addListener("drag", this.__onDrag, this);
    this.addListener("dragend", this.__onDragend, this);

    this.__xDirs = ["left", "right"];
    this.__yDirs = ["top", "bottom"];
  },

  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /** The threshold for the x-axis (in pixel) to activate scrolling at the edges. */
    dragScrollThresholdX :
    {
      check : "Integer",
      init : 30
    },

    /** The threshold for the y-axis (in pixel) to activate scrolling at the edges. */
    dragScrollThresholdY :
    {
      check : "Integer",
      init : 30
    },

    /** The factor for slowing down the scrolling. */
    dragScrollSlowDownFactor :
    {
      check : "Float",
      init : 0.1
    }
  },

  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __dragScrollTimer : null,
    __xDirs : null,
    __yDirs : null,

    /**
     * Finds the first scrollable parent (in the parent chain).
     *
     * @param widget {qx.ui.core.LayoutItem} The widget to start from.
     * @return {qx.ui.core.Widget} A scrollable widget.
     */
    _findScrollableParent : function(widget)
    {
      var cur = widget;
      if (cur === null) {
        return null;
      }

      while (cur.getLayoutParent()) {
        cur = cur.getLayoutParent();
        if (this._isScrollable(cur)) {
          return cur;
        }
      }
      return null;
    },

    /**
     * Whether the widget is scrollable.
     *
     * @param widget {qx.ui.core.Widget} The widget to check.
     * @return {Boolean} Whether the widget is scrollable.
     */
    _isScrollable : function(widget)
    {
      return qx.Class.hasMixin(widget.constructor, qx.ui.core.scroll.MScrollBarFactory);
    },

    /**
     * Gets the bounds of the given scrollable.
     *
     * @param scrollable {qx.ui.core.Widget} Scrollable which has scrollbar child controls.
     * @return {Map} A map with all four bounds (e.g. {"left":0, "top":20, "right":0, "bottom":80}).
     */
    _getBounds : function(scrollable)
    {
      var bounds = scrollable.getContentLocation();

      // the scrollable may dictate a nested widget for more precise bounds
      if (scrollable.getScrollAreaContainer) {
        bounds = scrollable.getScrollAreaContainer().getContentLocation();
      }

      return bounds;
    },

    /**
     * Gets the edge type or null if the mouse isn't within one of the thresholds.
     *
     * @param diff {Map} Difference map with all for edgeTypes.
     * @param thresholdX {Number} x-axis threshold.
     * @param thresholdY {Number} y-axis threshold.
     * @return {String} One of the four edgeTypes ('left', 'right', 'top', 'bottom').
     */
    _getEdgeType : function(diff, thresholdX, thresholdY)
    {
      if ((diff.left * -1) <= thresholdX && diff.left < 0) {
        return "left";
      } else if ((diff.top * -1) <= thresholdY && diff.top < 0) {
        return "top";
      } else if (diff.right <= thresholdX && diff.right > 0) {
        return "right";
      } else if (diff.bottom <= thresholdY && diff.bottom > 0) {
        return "bottom";
      } else {
        return null;
      }
    },

    /**
     * Gets the axis ('x' or 'y') by the edge type.
     *
     * @param edgeType {String} One of the four edgeTypes ('left', 'right', 'top', 'bottom').
     * @throws {Error} If edgeType is not one of the distinct four ones.
     * @return {String} Returns 'y' or 'x'.
     */
    _getAxis : function(edgeType)
    {
      if (this.__xDirs.indexOf(edgeType) !== -1) {
        return "x";
      } else if (this.__yDirs.indexOf(edgeType) !== -1) {
        return "y";
      } else {
        throw new Error("Invalid edge type given ("+edgeType+"). Must be: 'left', 'right', 'top' or 'bottom'");
      }
    },

    /**
     * Gets the threshold amount by edge type.
     *
     * @param edgeType {String} One of the four edgeTypes ('left', 'right', 'top', 'bottom').
     * @return {Number} The threshold of the x or y axis.
     */
    _getThresholdByEdgeType : function(edgeType) {
      if (this.__xDirs.indexOf(edgeType) !== -1) {
        return this.getDragScrollThresholdX();
      } else if(this.__yDirs.indexOf(edgeType) !== -1) {
        return this.getDragScrollThresholdY();
      }
    },

    /**
     * Whether the scrollbar is visible.
     *
     * @param scrollable {qx.ui.core.Widget} Scrollable which has scrollbar child controls.
     * @param axis {String} Can be 'y' or 'x'.
     * @return {Boolean} Whether the scrollbar is visible.
     */
    _isScrollbarVisible : function(scrollable, axis)
    {
      if (scrollable && scrollable._isChildControlVisible) {
        return scrollable._isChildControlVisible("scrollbar-"+axis);
      } else {
        return false;
      }
    },

    /**
     * Whether the scrollbar is exceeding it's maximum position.
     *
     * @param scrollbar {qx.ui.core.scroll.IScrollBar} Scrollbar to check.
     * @param axis {String} Can be 'y' or 'x'.
     * @param amount {Number} Amount to scroll which may be negative.
     * @return {Boolean} Whether the amount will exceed the scrollbar max position.
     */
    _isScrollbarExceedingMaxPos : function(scrollbar, axis, amount)
    {
      var newPos = 0;
      if (!scrollbar) {
        return true;
      }
      newPos = scrollbar.getPosition() + amount;
      return (newPos > scrollbar.getMaximum() || newPos < 0);
    },

    /**
     * Calculates the threshold exceedance (which may be negative).
     *
     * @param diff {Number} Difference value of one edgeType.
     * @param threshold {Number} x-axis or y-axis threshold.
     * @return {Number} Threshold exceedance amount (positive or negative).
     */
    _calculateThresholdExceedance : function(diff, threshold)
    {
      var amount = threshold - Math.abs(diff);
      return diff < 0 ? (amount * -1) : amount;
    },

    /**
     * Calculates the scroll amount (which may be negative).
     * The amount is influenced by the scrollbar size (bigger = faster)
     * the exceedanceAmount (bigger = faster) and the slowDownFactor.
     *
     * @param scrollbarSize {Number} Size of the scrollbar.
     * @param exceedanceAmount {Number} Threshold exceedance amount (positive or negative).
     * @return {Number} Scroll amount (positive or negative).
     */
    _calculateScrollAmount : function(scrollbarSize, exceedanceAmount)
    {
      return Math.floor(((scrollbarSize / 100) * exceedanceAmount) * this.getDragScrollSlowDownFactor());
    },

    /**
     * Scrolls the given scrollable on the given axis for the given amount.
     *
     * @param scrollable {qx.ui.core.Widget} Scrollable which has scrollbar child controls.
     * @param axis {String} Can be 'y' or 'x'.
     * @param exceedanceAmount {Number} Threshold exceedance amount (positive or negative).
     */
    _scrollBy : function(scrollable, axis, exceedanceAmount) {
      var scrollbar = scrollable.getChildControl("scrollbar-"+axis, true);
      if (!scrollbar) {
        return;
      }
      var bounds = scrollbar.getBounds(),
          scrollbarSize = axis === "x" ? bounds.width : bounds.height,
          amount = this._calculateScrollAmount(scrollbarSize, exceedanceAmount);

      if (this._isScrollbarExceedingMaxPos(scrollbar, axis, amount)) {
        this.__dragScrollTimer.stop();
      }

      scrollbar.scrollBy(amount);
    },

    /*
    ---------------------------------------------------------------------------
    EVENT HANDLERS
    ---------------------------------------------------------------------------
    */

    /**
     * Event handler for the drag event.
     *
     * @param e {qx.event.type.Drag} The drag event instance.
     */
    __onDrag : function(e)
    {
      if (this.__dragScrollTimer) {
        // stop last scroll action
        this.__dragScrollTimer.stop();
      }

      var scrollable = this._findScrollableParent(e.getOriginalTarget());

      if (scrollable) {
        var bounds = this._getBounds(scrollable),
            xPos = e.getDocumentLeft(),
            yPos = e.getDocumentTop(),
            diff = {
              "left": bounds.left - xPos,
              "right": bounds.right - xPos,
              "top": bounds.top - yPos,
              "bottom": bounds.bottom - yPos
            },
            edgeType = null,
            axis = "",
            exceedanceAmount = 0;

        edgeType = this._getEdgeType(diff, this.getDragScrollThresholdX(), this.getDragScrollThresholdY());
        if (!edgeType) {
          // return if not within edge threshold
          return;
        }
        axis = this._getAxis(edgeType);

        if (this._isScrollbarVisible(scrollable, axis)) {
          exceedanceAmount = this._calculateThresholdExceedance(diff[edgeType], this._getThresholdByEdgeType(edgeType));

          this.__dragScrollTimer = new qx.event.Timer(50);
          this.__dragScrollTimer.addListener("interval",
            function(scrollable, axis, amount) {
              this._scrollBy(scrollable, axis, amount);
            }.bind(this, scrollable, axis, exceedanceAmount));
          this.__dragScrollTimer.start();
        }
      }
    },

    /**
     * Event handler for the dragend event.
     *
     * @param e {qx.event.type.Drag} The drag event instance.
     */
    __onDragend : function(e)
    {
      if (this.__dragScrollTimer) {
        this.__dragScrollTimer.stop();
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's left-level directory for details.

   Authors:
     * Fabian Jakobs (fjakobs)

************************************************************************ */

qx.core.Environment.add("qx.nativeScrollBars", false);

/**
 * Include this widget if you want to create scrollbars depending on the global
 * "qx.nativeScrollBars" setting.
 */
qx.Mixin.define("qx.ui.core.scroll.MScrollBarFactory",
{
  members :
  {
    /**
     * Creates a new scrollbar. This can either be a styled qooxdoo scrollbar
     * or a native browser scrollbar.
     *
     * @param orientation {String?"horizontal"} The initial scroll bar orientation
     * @return {qx.ui.core.scroll.IScrollBar} The scrollbar instance
     */
    _createScrollBar : function(orientation)
    {
      if (qx.core.Environment.get("qx.nativeScrollBars")) {
        return new qx.ui.core.scroll.NativeScrollBar(orientation);
      } else {
        return new qx.ui.core.scroll.ScrollBar(orientation);
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's left-level directory for details.

   Authors:
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * All widget used as scrollbars must implement this interface.
 */
qx.Interface.define("qx.ui.core.scroll.IScrollBar",
{
  events :
  {
    /** Fired if the user scroll */
    "scroll" : "qx.event.type.Data",
    /** Fired as soon as the scroll animation ended. */
    "scrollAnimationEnd": 'qx.event.type.Event'
  },


  properties :
  {
    /**
     * The scroll bar orientation
     */
    orientation : {},


    /**
     * The maximum value (difference between available size and
     * content size).
     */
    maximum : {},


    /**
     * Position of the scrollbar (which means the scroll left/top of the
     * attached area's pane)
     *
     * Strictly validates according to {@link #maximum}.
     * Does not apply any correction to the incoming value. If you depend
     * on this, please use {@link #scrollTo} instead.
     */
    position : {},


    /**
     * Factor to apply to the width/height of the knob in relation
     * to the dimension of the underlying area.
     */
    knobFactor : {}
  },


  members :
  {
    /**
     * Scrolls to the given position.
     *
     * This method automatically corrects the given position to respect
     * the {@link #maximum}.
     *
     * @param position {Integer} Scroll to this position. Must be greater zero.
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    scrollTo : function(position, duration) {
      this.assertNumber(position);
    },


    /**
     * Scrolls by the given offset.
     *
     * This method automatically corrects the given position to respect
     * the {@link #maximum}.
     *
     * @param offset {Integer} Scroll by this offset
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    scrollBy : function(offset, duration) {
      this.assertNumber(offset);
    },


    /**
     * Scrolls by the given number of steps.
     *
     * This method automatically corrects the given position to respect
     * the {@link #maximum}.
     *
     * @param steps {Integer} Number of steps
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    scrollBySteps : function(steps, duration) {
      this.assertNumber(steps);
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's left-level directory for details.

   Authors:
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * The scroll bar widget wraps the native browser scroll bars as a qooxdoo widget.
 * It can be uses instead of the styled qooxdoo scroll bars.
 *
 * Scroll bars are used by the {@link qx.ui.container.Scroll} container. Usually
 * a scroll bar is not used directly.
 *
 * *Example*
 *
 * Here is a little example of how to use the widget.
 *
 * <pre class='javascript'>
 *   var scrollBar = new qx.ui.core.scroll.NativeScrollBar("horizontal");
 *   scrollBar.set({
 *     maximum: 500
 *   })
 *   this.getRoot().add(scrollBar);
 * </pre>
 *
 * This example creates a horizontal scroll bar with a maximum value of 500.
 *
 * *External Documentation*
 *
 * <a href='http://manual.qooxdoo.org/${qxversion}/pages/widget/scrollbar.html' target='_blank'>
 * Documentation of this widget in the qooxdoo manual.</a>
 */
qx.Class.define("qx.ui.core.scroll.NativeScrollBar",
{
  extend : qx.ui.core.Widget,
  implement : qx.ui.core.scroll.IScrollBar,


  /**
   * @param orientation {String?"horizontal"} The initial scroll bar orientation
   */
  construct : function(orientation)
  {
    this.base(arguments);

    this.addState("native");

    this.getContentElement().addListener("scroll", this._onScroll, this);
    this.addListener("mousedown", this._stopPropagation, this);
    this.addListener("mouseup", this._stopPropagation, this);
    this.addListener("mousemove", this._stopPropagation, this);
    this.addListener("appear", this._onAppear, this);

    this.getContentElement().add(this._getScrollPaneElement());
    this.getContentElement().setStyle("box-sizing", "content-box");

    // Configure orientation
    if (orientation != null) {
      this.setOrientation(orientation);
    } else {
      this.initOrientation();
    }
  },


  events : {
    /**
     * Fired as soon as the scroll animation ended.
     */
    scrollAnimationEnd: 'qx.event.type.Event'
  },


  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "scrollbar"
    },


    // interface implementation
    orientation :
    {
      check : [ "horizontal", "vertical" ],
      init : "horizontal",
      apply : "_applyOrientation"
    },


    // interface implementation
    maximum :
    {
      check : "PositiveInteger",
      apply : "_applyMaximum",
      init : 100
    },


    // interface implementation
    position :
    {
      check : "Number",
      init : 0,
      apply : "_applyPosition",
      event : "scroll"
    },


    /**
     * Step size for each click on the up/down or left/right buttons.
     */
    singleStep :
    {
      check : "Integer",
      init : 20
    },


    // interface implementation
    knobFactor :
    {
      check : "PositiveNumber",
      nullable : true
    }
  },


  members :
  {
    __isHorizontal : null,
    __scrollPaneElement : null,
    __requestId : null,

    __scrollAnimationframe : null,


    /**
     * Get the scroll pane html element.
     *
     * @return {qx.html.Element} The element
     */
    _getScrollPaneElement : function()
    {
      if (!this.__scrollPaneElement) {
        this.__scrollPaneElement = new qx.html.Element();
      }
      return this.__scrollPaneElement;
    },

    /*
    ---------------------------------------------------------------------------
      WIDGET API
    ---------------------------------------------------------------------------
    */

    // overridden
    renderLayout : function(left, top, width, height)
    {
      var changes = this.base(arguments, left, top, width, height);

      this._updateScrollBar();
      return changes;
    },


    // overridden
    _getContentHint : function()
    {
      var scrollbarWidth = qx.bom.element.Scroll.getScrollbarWidth();
      return {
        width: this.__isHorizontal ? 100 : scrollbarWidth,
        maxWidth: this.__isHorizontal ? null : scrollbarWidth,
        minWidth: this.__isHorizontal ? null : scrollbarWidth,
        height: this.__isHorizontal ? scrollbarWidth : 100,
        maxHeight: this.__isHorizontal ? scrollbarWidth : null,
        minHeight: this.__isHorizontal ? scrollbarWidth : null
      }
    },


    // overridden
    _applyEnabled : function(value, old)
    {
      this.base(arguments, value, old);
      this._updateScrollBar();
    },


    /*
    ---------------------------------------------------------------------------
      PROPERTY APPLY ROUTINES
    ---------------------------------------------------------------------------
    */

    // property apply
    _applyMaximum : function(value) {
      this._updateScrollBar();
    },


    // property apply
    _applyPosition : function(value)
    {
      var content = this.getContentElement();

      if (this.__isHorizontal) {
        content.scrollToX(value)
      } else {
        content.scrollToY(value);
      }
    },


    // property apply
    _applyOrientation : function(value, old)
    {
      var isHorizontal = this.__isHorizontal = value === "horizontal";

      this.set({
        allowGrowX : isHorizontal,
        allowShrinkX : isHorizontal,
        allowGrowY : !isHorizontal,
        allowShrinkY : !isHorizontal
      });

      if (isHorizontal) {
        this.replaceState("vertical", "horizontal");
      } else {
        this.replaceState("horizontal", "vertical");
      }

      this.getContentElement().setStyles({
        overflowX: isHorizontal ? "scroll" : "hidden",
        overflowY: isHorizontal ? "hidden" : "scroll"
      });

      // Update layout
      qx.ui.core.queue.Layout.add(this);
    },


    /**
     * Update the scroll bar according to its current size, max value and
     * enabled state.
     */
    _updateScrollBar : function()
    {
      var isHorizontal = this.__isHorizontal;

      var bounds = this.getBounds();
      if (!bounds) {
        return;
      }

      if (this.isEnabled())
      {
        var containerSize = isHorizontal ? bounds.width : bounds.height;
        var innerSize = this.getMaximum() + containerSize;
      } else {
        innerSize = 0;
      }

      // Scrollbars don't work properly in IE if the element with overflow has
      // excatly the size of the scrollbar. Thus we move the element one pixel
      // out of the view and increase the size by one.
      if (qx.core.Environment.get("engine.name") == "mshtml")
      {
        var bounds = this.getBounds();
        this.getContentElement().setStyles({
          left: (isHorizontal ? bounds.left : (bounds.left -1)) + "px",
          top: (isHorizontal ? (bounds.top - 1) : bounds.top) + "px",
          width: (isHorizontal ? bounds.width : bounds.width + 1) + "px",
          height: (isHorizontal ? bounds.height + 1 : bounds.height) + "px"
        });
      }

      this._getScrollPaneElement().setStyles({
        left: 0,
        top: 0,
        width: (isHorizontal ? innerSize : 1) + "px",
        height: (isHorizontal ? 1 : innerSize) + "px"
      });

      this.updatePosition(this.getPosition());
    },


    // interface implementation
    scrollTo : function(position, duration) {
      // if a user sets a new position, stop any animation
      this.stopScrollAnimation();

      if (duration) {
        var from = this.getPosition();

        this.__scrollAnimationframe = new qx.bom.AnimationFrame();

        this.__scrollAnimationframe.on("frame", function(timePassed) {
          var newPos = parseInt(timePassed/duration * (position - from) + from);
          this.updatePosition(newPos);
        }, this);

        this.__scrollAnimationframe.on("end", function() {
          this.setPosition(Math.max(0, Math.min(this.getMaximum(), position)));
          this.__scrollAnimationframe = null;
          this.fireEvent("scrollAnimationEnd");
        }, this);

        this.__scrollAnimationframe.startSequence(duration);
      } else {
        this.updatePosition(position);
      }
    },


    /**
     * Helper to set the new position taking care of min and max values.
     * @param position {Number} The new position.
     */
    updatePosition : function(position) {
      this.setPosition(Math.max(0, Math.min(this.getMaximum(), position)));
    },


    // interface implementation
    scrollBy : function(offset, duration) {
      this.scrollTo(this.getPosition() + offset, duration)
    },


    // interface implementation
    scrollBySteps : function(steps, duration)
    {
      var size = this.getSingleStep();
      this.scrollBy(steps * size, duration);
    },


    /**
     * If a scroll animation is running, it will be stopped.
     */
    stopScrollAnimation : function() {
      if (this.__scrollAnimationframe) {
        this.__scrollAnimationframe.cancelSequence();
        this.__scrollAnimationframe = null;
      }
    },


    /**
     * Scroll event handler
     *
     * @param e {qx.event.type.Event} the scroll event
     */
    _onScroll : function(e)
    {
      var container = this.getContentElement();
      var position = this.__isHorizontal ? container.getScrollX() : container.getScrollY();
      this.setPosition(position);
    },


    /**
     * Listener for appear which ensured the scroll bar is positioned right
     * on appear.
     *
     * @param e {qx.event.type.Data} Incoming event object
     */
    _onAppear : function(e) {
      this._applyPosition(this.getPosition());
    },


    /**
     * Stops propagation on the given even
     *
     * @param e {qx.event.type.Event} the event
     */
    _stopPropagation : function(e) {
      e.stopPropagation();
    }
  },


  destruct : function() {
    this._disposeObjects("__scrollPaneElement");
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's left-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * The scroll bar widget, is a special slider, which is used in qooxdoo instead
 * of the native browser scroll bars.
 *
 * Scroll bars are used by the {@link qx.ui.container.Scroll} container. Usually
 * a scroll bar is not used directly.
 *
 * @childControl slider {qx.ui.core.scroll.ScrollSlider} scroll slider component
 * @childControl button-begin {qx.ui.form.RepeatButton} button to scroll to top
 * @childControl button-end {qx.ui.form.RepeatButton} button to scroll to bottom
 *
 * *Example*
 *
 * Here is a little example of how to use the widget.
 *
 * <pre class='javascript'>
 *   var scrollBar = new qx.ui.core.scroll.ScrollBar("horizontal");
 *   scrollBar.set({
 *     maximum: 500
 *   })
 *   this.getRoot().add(scrollBar);
 * </pre>
 *
 * This example creates a horizontal scroll bar with a maximum value of 500.
 *
 * *External Documentation*
 *
 * <a href='http://manual.qooxdoo.org/${qxversion}/pages/widget/scrollbar.html' target='_blank'>
 * Documentation of this widget in the qooxdoo manual.</a>
 */
qx.Class.define("qx.ui.core.scroll.ScrollBar",
{
  extend : qx.ui.core.Widget,
  implement : qx.ui.core.scroll.IScrollBar,



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param orientation {String?"horizontal"} The initial scroll bar orientation
   */
  construct : function(orientation)
  {
    this.base(arguments);

    // Create child controls
    this._createChildControl("button-begin");
    this._createChildControl("slider").addListener("resize", this._onResizeSlider, this);
    this._createChildControl("button-end");

    // Configure orientation
    if (orientation != null) {
      this.setOrientation(orientation);
    } else {
      this.initOrientation();
    }
  },


  events : {
    /** Change event for the value. */
    "scrollAnimationEnd": "qx.event.type.Event"
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "scrollbar"
    },


    /**
     * The scroll bar orientation
     */
    orientation :
    {
      check : [ "horizontal", "vertical" ],
      init : "horizontal",
      apply : "_applyOrientation"
    },


    /**
     * The maximum value (difference between available size and
     * content size).
     */
    maximum :
    {
      check : "PositiveInteger",
      apply : "_applyMaximum",
      init : 100
    },


    /**
     * Position of the scrollbar (which means the scroll left/top of the
     * attached area's pane)
     *
     * Strictly validates according to {@link #maximum}.
     * Does not apply any correction to the incoming value. If you depend
     * on this, please use {@link #scrollTo} instead.
     */
    position :
    {
      check : "qx.lang.Type.isNumber(value)&&value>=0&&value<=this.getMaximum()",
      init : 0,
      apply : "_applyPosition",
      event : "scroll"
    },


    /**
     * Step size for each click on the up/down or left/right buttons.
     */
    singleStep :
    {
      check : "Integer",
      init : 20
    },


    /**
     * The amount to increment on each event. Typically corresponds
     * to the user pressing <code>PageUp</code> or <code>PageDown</code>.
     */
    pageStep :
    {
      check : "Integer",
      init : 10,
      apply : "_applyPageStep"
    },


    /**
     * Factor to apply to the width/height of the knob in relation
     * to the dimension of the underlying area.
     */
    knobFactor :
    {
      check : "PositiveNumber",
      apply : "_applyKnobFactor",
      nullable : true
    }
  },





  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __offset : 2,
    __originalMinSize : 0,


    // overridden
    _computeSizeHint : function() {
      var hint = this.base(arguments);
      if (this.getOrientation() === "horizontal") {
        this.__originalMinSize = hint.minWidth;
        hint.minWidth = 0;
      } else {
        this.__originalMinSize = hint.minHeight;
        hint.minHeight = 0;
      }
      return hint;
    },


    // overridden
    renderLayout : function(left, top, width, height) {
      var changes = this.base(arguments, left, top, width, height);
      var horizontal = this.getOrientation() === "horizontal";
      if (this.__originalMinSize >= (horizontal ? width : height)) {
        this.getChildControl("button-begin").setVisibility("hidden");
        this.getChildControl("button-end").setVisibility("hidden");
      } else {
        this.getChildControl("button-begin").setVisibility("visible");
        this.getChildControl("button-end").setVisibility("visible");
      }

      return changes
    },

    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "slider":
          control = new qx.ui.core.scroll.ScrollSlider();
          control.setPageStep(100);
          control.setFocusable(false);
          control.addListener("changeValue", this._onChangeSliderValue, this);
          control.addListener("slideAnimationEnd", this._onSlideAnimationEnd, this);
          this._add(control, {flex: 1});
          break;

        case "button-begin":
          // Top/Left Button
          control = new qx.ui.form.RepeatButton();
          control.setFocusable(false);
          control.addListener("execute", this._onExecuteBegin, this);
          this._add(control);
          break;

        case "button-end":
          // Bottom/Right Button
          control = new qx.ui.form.RepeatButton();
          control.setFocusable(false);
          control.addListener("execute", this._onExecuteEnd, this);
          this._add(control);
          break;
      }

      return control || this.base(arguments, id);
    },




    /*
    ---------------------------------------------------------------------------
      PROPERTY APPLY ROUTINES
    ---------------------------------------------------------------------------
    */

    // property apply
    _applyMaximum : function(value) {
      this.getChildControl("slider").setMaximum(value);
    },


    // property apply
    _applyPosition : function(value) {
      this.getChildControl("slider").setValue(value);
    },


    // property apply
    _applyKnobFactor : function(value) {
      this.getChildControl("slider").setKnobFactor(value);
    },


    // property apply
    _applyPageStep : function(value) {
      this.getChildControl("slider").setPageStep(value);
    },


    // property apply
    _applyOrientation : function(value, old)
    {
      // Dispose old layout
      var oldLayout = this._getLayout();
      if (oldLayout) {
        oldLayout.dispose();
      }

      // Reconfigure
      if (value === "horizontal")
      {
        this._setLayout(new qx.ui.layout.HBox());

        this.setAllowStretchX(true);
        this.setAllowStretchY(false);

        this.replaceState("vertical", "horizontal");

        this.getChildControl("button-begin").replaceState("up", "left");
        this.getChildControl("button-end").replaceState("down", "right");
      }
      else
      {
        this._setLayout(new qx.ui.layout.VBox());

        this.setAllowStretchX(false);
        this.setAllowStretchY(true);

        this.replaceState("horizontal", "vertical");

        this.getChildControl("button-begin").replaceState("left", "up");
        this.getChildControl("button-end").replaceState("right", "down");
      }

      // Sync slider orientation
      this.getChildControl("slider").setOrientation(value);
    },





    /*
    ---------------------------------------------------------------------------
      METHOD REDIRECTION TO SLIDER
    ---------------------------------------------------------------------------
    */

    /**
     * Scrolls to the given position.
     *
     * This method automatically corrects the given position to respect
     * the {@link #maximum}.
     *
     * @param position {Integer} Scroll to this position. Must be greater zero.
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    scrollTo : function(position, duration) {
      this.getChildControl("slider").slideTo(position, duration);
    },


    /**
     * Scrolls by the given offset.
     *
     * This method automatically corrects the given position to respect
     * the {@link #maximum}.
     *
     * @param offset {Integer} Scroll by this offset
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    scrollBy : function(offset, duration) {
      this.getChildControl("slider").slideBy(offset, duration);
    },


    /**
     * Scrolls by the given number of steps.
     *
     * This method automatically corrects the given position to respect
     * the {@link #maximum}.
     *
     * @param steps {Integer} Number of steps
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    scrollBySteps : function(steps, duration) {
      var size = this.getSingleStep();
      this.getChildControl("slider").slideBy(steps * size, duration);
    },


    /**
     * Updates the position property considering the minimum and maximum values.
     * @param position {Number} The new position.
     */
    updatePosition : function(position) {
      this.getChildControl("slider").updatePosition(position);
    },


    /**
     * If a scroll animation is running, it will be stopped.
     */
    stopScrollAnimation : function() {
      this.getChildControl("slider").stopSlideAnimation();
    },


    /*
    ---------------------------------------------------------------------------
      EVENT LISTENER
    ---------------------------------------------------------------------------
    */

    /**
     * Executed when the up/left button is executed (pressed)
     *
     * @param e {qx.event.type.Event} Execute event of the button
     */
    _onExecuteBegin : function(e) {
      this.scrollBy(-this.getSingleStep(), 50);
    },


    /**
     * Executed when the down/right button is executed (pressed)
     *
     * @param e {qx.event.type.Event} Execute event of the button
     */
    _onExecuteEnd : function(e) {
      this.scrollBy(this.getSingleStep(), 50);
    },


    /**
     * Change listener for slider animation end.
     */
    _onSlideAnimationEnd : function() {
      this.fireEvent("scrollAnimationEnd");
    },


    /**
     * Change listener for slider value changes.
     *
     * @param e {qx.event.type.Data} The change event object
     */
    _onChangeSliderValue : function(e) {
      this.setPosition(e.getData());
    },

    /**
     * Hide the knob of the slider if the slidebar is too small or show it
     * otherwise.
     *
     * @param e {qx.event.type.Data} event object
     */
    _onResizeSlider : function(e)
    {
      var knob = this.getChildControl("slider").getChildControl("knob");
      var knobHint = knob.getSizeHint();
      var hideKnob = false;
      var sliderSize = this.getChildControl("slider").getInnerSize();

      if (this.getOrientation() == "vertical")
      {
        if (sliderSize.height  < knobHint.minHeight + this.__offset) {
          hideKnob = true;
        }
      }
      else
      {
        if (sliderSize.width  < knobHint.minWidth + this.__offset) {
          hideKnob = true;
        }
      }

      if (hideKnob) {
        knob.exclude();
      } else {
        knob.show();
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's left-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * The Slider widget provides a vertical or horizontal slider.
 *
 * The Slider is the classic widget for controlling a bounded value.
 * It lets the user move a slider handle along a horizontal or vertical
 * groove and translates the handle's position into an integer value
 * within the defined range.
 *
 * The Slider has very few of its own functions.
 * The most useful functions are slideTo() to set the slider directly to some
 * value; setSingleStep(), setPageStep() to set the steps; and setMinimum()
 * and setMaximum() to define the range of the slider.
 *
 * A slider accepts focus on Tab and provides both a mouse wheel and
 * a keyboard interface. The keyboard interface is the following:
 *
 * * Left/Right move a horizontal slider by one single step.
 * * Up/Down move a vertical slider by one single step.
 * * PageUp moves up one page.
 * * PageDown moves down one page.
 * * Home moves to the start (minimum).
 * * End moves to the end (maximum).
 *
 * Here are the main properties of the class:
 *
 * # <code>value</code>: The bounded integer that {@link qx.ui.form.INumberForm}
 * maintains.
 * # <code>minimum</code>: The lowest possible value.
 * # <code>maximum</code>: The highest possible value.
 * # <code>singleStep</code>: The smaller of two natural steps that an abstract
 * sliders provides and typically corresponds to the user pressing an arrow key.
 * # <code>pageStep</code>: The larger of two natural steps that an abstract
 * slider provides and typically corresponds to the user pressing PageUp or
 * PageDown.
 *
 * @childControl knob {qx.ui.core.Widget} knob to set the value of the slider
 */
qx.Class.define("qx.ui.form.Slider",
{
  extend : qx.ui.core.Widget,
  implement : [
    qx.ui.form.IForm,
    qx.ui.form.INumberForm,
    qx.ui.form.IRange
  ],
  include : [qx.ui.form.MForm],


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param orientation {String?"horizontal"} Configure the
   * {@link #orientation} property
   */
  construct : function(orientation)
  {
    this.base(arguments);

    // Force canvas layout
    this._setLayout(new qx.ui.layout.Canvas());

    // Add listeners
    this.addListener("keypress", this._onKeyPress);
    this.addListener("mousewheel", this._onMouseWheel);
    this.addListener("mousedown", this._onMouseDown);
    this.addListener("mouseup", this._onMouseUp);
    this.addListener("losecapture", this._onMouseUp);
    this.addListener("resize", this._onUpdate);

    // Stop events
    this.addListener("contextmenu", this._onStopEvent);
    this.addListener("click", this._onStopEvent);
    this.addListener("dblclick", this._onStopEvent);

    // Initialize orientation
    if (orientation != null) {
      this.setOrientation(orientation);
    } else {
      this.initOrientation();
    }
  },


  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events : {
    /**
     * Change event for the value.
     */
    changeValue: 'qx.event.type.Data',

    /** Fired as soon as the slide animation ended. */
    slideAnimationEnd: 'qx.event.type.Event'
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "slider"
    },


    // overridden
    focusable :
    {
      refine : true,
      init : true
    },


    /** Whether the slider is horizontal or vertical. */
    orientation :
    {
      check : [ "horizontal", "vertical" ],
      init : "horizontal",
      apply : "_applyOrientation"
    },


    /**
     * The current slider value.
     *
     * Strictly validates according to {@link #minimum} and {@link #maximum}.
     * Do not apply any value correction to the incoming value. If you depend
     * on this, please use {@link #slideTo} instead.
     */
    value :
    {
      check : "typeof value==='number'&&value>=this.getMinimum()&&value<=this.getMaximum()",
      init : 0,
      apply : "_applyValue",
      nullable: true
    },


    /**
     * The minimum slider value (may be negative). This value must be smaller
     * than {@link #maximum}.
     */
    minimum :
    {
      check : "Integer",
      init : 0,
      apply : "_applyMinimum",
      event: "changeMinimum"
    },


    /**
     * The maximum slider value (may be negative). This value must be larger
     * than {@link #minimum}.
     */
    maximum :
    {
      check : "Integer",
      init : 100,
      apply : "_applyMaximum",
      event : "changeMaximum"
    },


    /**
     * The amount to increment on each event. Typically corresponds
     * to the user pressing an arrow key.
     */
    singleStep :
    {
      check : "Integer",
      init : 1
    },


    /**
     * The amount to increment on each event. Typically corresponds
     * to the user pressing <code>PageUp</code> or <code>PageDown</code>.
     */
    pageStep :
    {
      check : "Integer",
      init : 10
    },


    /**
     * Factor to apply to the width/height of the knob in relation
     * to the dimension of the underlying area.
     */
    knobFactor :
    {
      check : "Number",
      apply : "_applyKnobFactor",
      nullable : true
    }
  },


  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {

    __sliderLocation : null,
    __knobLocation : null,
    __knobSize : null,
    __dragMode : null,
    __dragOffset : null,
    __trackingMode : null,
    __trackingDirection : null,
    __trackingEnd : null,
    __timer : null,

    // event delay stuff during drag
    __dragTimer: null,
    __lastValueEvent: null,
    __dragValue: null,

    __scrollAnimationframe : null,


    // overridden
    /**
     * @lint ignoreReferenceField(_forwardStates)
     */
    _forwardStates : {
      invalid : true
    },


    // overridden
    renderLayout : function(left, top, width, height) {
      this.base(arguments, left, top, width, height);
      // make sure the layout engine does not override the knob position
      this._updateKnobPosition();
    },


    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "knob":
          control = new qx.ui.core.Widget();

          control.addListener("resize", this._onUpdate, this);
          control.addListener("mouseover", this._onMouseOver);
          control.addListener("mouseout", this._onMouseOut);
          this._add(control);
          break;
      }

      return control || this.base(arguments, id);
    },


    /*
    ---------------------------------------------------------------------------
      EVENT HANDLER
    ---------------------------------------------------------------------------
    */


    /**
     * Event handler for mouseover events at the knob child control.
     *
     * Adds the 'hovered' state
     *
     * @param e {qx.event.type.Mouse} Incoming mouse event
     */
    _onMouseOver : function(e) {
      this.addState("hovered");
    },


    /**
     * Event handler for mouseout events at the knob child control.
     *
     * Removes the 'hovered' state
     *
     * @param e {qx.event.type.Mouse} Incoming mouse event
     */
    _onMouseOut : function(e) {
      this.removeState("hovered");
    },


    /**
     * Listener of mousewheel event
     *
     * @param e {qx.event.type.Mouse} Incoming event object
     */
    _onMouseWheel : function(e)
    {
      var axis = this.getOrientation() === "horizontal" ? "x" : "y";
      var delta = e.getWheelDelta(axis);

      if (qx.event.handler.MouseEmulation.ON) {
        this.slideBy(-delta);
      } else {
        var direction =  delta > 0 ? 1 : delta < 0 ? -1 : 0;
        this.slideBy(direction * this.getSingleStep());
      }

      e.stop();
    },


    /**
     * Event handler for keypress events.
     *
     * Adds support for arrow keys, page up, page down, home and end keys.
     *
     * @param e {qx.event.type.KeySequence} Incoming keypress event
     */
    _onKeyPress : function(e)
    {
      var isHorizontal = this.getOrientation() === "horizontal";
      var backward = isHorizontal ? "Left" : "Up";
      var forward = isHorizontal ? "Right" : "Down";

      switch(e.getKeyIdentifier())
      {
        case forward:
          this.slideForward();
          break;

        case backward:
          this.slideBack();
          break;

        case "PageDown":
          this.slidePageForward(100);
          break;

        case "PageUp":
          this.slidePageBack(100);
          break;

        case "Home":
          this.slideToBegin(200);
          break;

        case "End":
          this.slideToEnd(200);
          break;

        default:
          return;
      }

      // Stop processed events
      e.stop();
    },


    /**
     * Listener of mousedown event. Initializes drag or tracking mode.
     *
     * @param e {qx.event.type.Mouse} Incoming event object
     */
    _onMouseDown : function(e)
    {
      // this can happen if the user releases the button while dragging outside
      // of the browser viewport
      if (this.__dragMode) {
        return;
      }

      var isHorizontal = this.__isHorizontal;
      var knob = this.getChildControl("knob");

      var locationProperty = isHorizontal ? "left" : "top";

      var cursorLocation = isHorizontal ? e.getDocumentLeft() : e.getDocumentTop();

      var decorator = this.getDecorator();
      decorator = qx.theme.manager.Decoration.getInstance().resolve(decorator);
      if (isHorizontal) {
        var decoratorPadding = decorator ? decorator.getInsets().left : 0;
        var padding = (this.getPaddingLeft() || 0) + decoratorPadding;
      } else {
        var decoratorPadding = decorator ? decorator.getInsets().top : 0;
        var padding = (this.getPaddingTop() || 0) + decoratorPadding;
      }

      var sliderLocation = this.__sliderLocation = qx.bom.element.Location.get(this.getContentElement().getDomElement())[locationProperty];
      sliderLocation += padding;

      var knobLocation = this.__knobLocation = qx.bom.element.Location.get(knob.getContentElement().getDomElement())[locationProperty];

      if (e.getTarget() === knob)
      {
        // Switch into drag mode
        this.__dragMode = true;
        if (!this.__dragTimer){
          // create a timer to fire delayed dragging events if dragging stops.
          this.__dragTimer = new qx.event.Timer(100);
          this.__dragTimer.addListener("interval", this._fireValue, this);
        }
        this.__dragTimer.start();
        // Compute dragOffset (includes both: inner position of the widget and
        // cursor position on knob)
        this.__dragOffset = cursorLocation + sliderLocation - knobLocation;

        // add state
        knob.addState("pressed");
      }
      else
      {
        // Switch into tracking mode
        this.__trackingMode = true;

        // Detect tracking direction
        this.__trackingDirection = cursorLocation <= knobLocation ? -1 : 1;

        // Compute end value
        this.__computeTrackingEnd(e);

        // Directly call interval method once
        this._onInterval();

        // Initialize timer (when needed)
        if (!this.__timer)
        {
          this.__timer = new qx.event.Timer(100);
          this.__timer.addListener("interval", this._onInterval, this);
        }

        // Start timer
        this.__timer.start();
      }

      // Register move listener
      this.addListener("mousemove", this._onMouseMove);

      // Activate capturing
      this.capture();

      // Stop event
      e.stopPropagation();
    },


    /**
     * Listener of mouseup event. Used for cleanup of previously
     * initialized modes.
     *
     * @param e {qx.event.type.Mouse} Incoming event object
     */
    _onMouseUp : function(e)
    {
      if (this.__dragMode)
      {
        // Release capture mode
        this.releaseCapture();

        // Cleanup status flags
        delete this.__dragMode;

        // as we come out of drag mode, make
        // sure content gets synced
        this.__dragTimer.stop();
        this._fireValue();

        delete this.__dragOffset;

        // remove state
        this.getChildControl("knob").removeState("pressed");

        // it's necessary to check whether the mouse cursor is over the knob widget to be able to
        // to decide whether to remove the 'hovered' state.
        if (e.getType() === "mouseup")
        {
          var deltaSlider;
          var deltaPosition;
          var positionSlider;

          if (this.__isHorizontal)
          {
            deltaSlider = e.getDocumentLeft() - (this._valueToPosition(this.getValue()) + this.__sliderLocation);

            positionSlider = qx.bom.element.Location.get(this.getContentElement().getDomElement())["top"];
            deltaPosition = e.getDocumentTop() - (positionSlider + this.getChildControl("knob").getBounds().top);
          }
          else
          {
            deltaSlider = e.getDocumentTop() - (this._valueToPosition(this.getValue()) + this.__sliderLocation);

            positionSlider = qx.bom.element.Location.get(this.getContentElement().getDomElement())["left"];
            deltaPosition = e.getDocumentLeft() - (positionSlider + this.getChildControl("knob").getBounds().left);
          }

          if (deltaPosition < 0 || deltaPosition > this.__knobSize ||
              deltaSlider < 0 || deltaSlider > this.__knobSize) {
            this.getChildControl("knob").removeState("hovered");
          }
        }

      }
      else if (this.__trackingMode)
      {
        // Stop timer interval
        this.__timer.stop();

        // Release capture mode
        this.releaseCapture();

        // Cleanup status flags
        delete this.__trackingMode;
        delete this.__trackingDirection;
        delete this.__trackingEnd;
      }

      // Remove move listener again
      this.removeListener("mousemove", this._onMouseMove);

      // Stop event
      if (e.getType() === "mouseup") {
        e.stopPropagation();
      }
    },


    /**
     * Listener of mousemove event for the knob. Only used in drag mode.
     *
     * @param e {qx.event.type.Mouse} Incoming event object
     */
    _onMouseMove : function(e)
    {
      if (this.__dragMode)
      {
        var dragStop = this.__isHorizontal ?
          e.getDocumentLeft() : e.getDocumentTop();
        var position = dragStop - this.__dragOffset;

        this.slideTo(this._positionToValue(position));
      }
      else if (this.__trackingMode)
      {
        // Update tracking end on mousemove
        this.__computeTrackingEnd(e);
      }

      // Stop event
      e.stopPropagation();
    },


    /**
     * Listener of interval event by the internal timer. Only used
     * in tracking sequences.
     *
     * @param e {qx.event.type.Event} Incoming event object
     */
    _onInterval : function(e)
    {
      // Compute new value
      var value = this.getValue() + (this.__trackingDirection * this.getPageStep());

      // Limit value
      if (value < this.getMinimum()) {
        value = this.getMinimum();
      } else if (value > this.getMaximum()) {
        value = this.getMaximum();
      }

      // Stop at tracking position (where the mouse is pressed down)
      var slideBack = this.__trackingDirection == -1;
      if ((slideBack && value <= this.__trackingEnd) || (!slideBack && value >= this.__trackingEnd)) {
        value = this.__trackingEnd;
      }

      // Finally slide to the desired position
      this.slideTo(value);
    },


    /**
     * Listener of resize event for both the slider itself and the knob.
     *
     * @param e {qx.event.type.Data} Incoming event object
     */
    _onUpdate : function(e)
    {
      // Update sliding space
      var availSize = this.getInnerSize();
      var knobSize = this.getChildControl("knob").getBounds();
      var sizeProperty = this.__isHorizontal ? "width" : "height";

      // Sync knob size
      this._updateKnobSize();

      // Store knob size
      this.__slidingSpace = availSize[sizeProperty] - knobSize[sizeProperty];
      this.__knobSize = knobSize[sizeProperty];

      // Update knob position (sliding space must be updated first)
      this._updateKnobPosition();
    },






    /*
    ---------------------------------------------------------------------------
      UTILS
    ---------------------------------------------------------------------------
    */

    /** @type {Boolean} Whether the slider is laid out horizontally */
    __isHorizontal : false,


    /**
     * @type {Integer} Available space for knob to slide on, computed on resize of
     * the widget
     */
    __slidingSpace : 0,


    /**
     * Computes the value where the tracking should end depending on
     * the current mouse position.
     *
     * @param e {qx.event.type.Mouse} Incoming mouse event
     */
    __computeTrackingEnd : function(e)
    {
      var isHorizontal = this.__isHorizontal;
      var cursorLocation = isHorizontal ? e.getDocumentLeft() : e.getDocumentTop();
      var sliderLocation = this.__sliderLocation;
      var knobLocation = this.__knobLocation;
      var knobSize = this.__knobSize;

      // Compute relative position
      var position = cursorLocation - sliderLocation;
      if (cursorLocation >= knobLocation) {
        position -= knobSize;
      }

      // Compute stop value
      var value = this._positionToValue(position);

      var min = this.getMinimum();
      var max = this.getMaximum();

      if (value < min) {
        value = min;
      } else if (value > max) {
        value = max;
      } else {
        var old = this.getValue();
        var step = this.getPageStep();
        var method = this.__trackingDirection < 0 ? "floor" : "ceil";

        // Fix to page step
        value = old + (Math[method]((value - old) / step) * step);
      }

      // Store value when undefined, otherwise only when it follows the
      // current direction e.g. goes up or down
      if (this.__trackingEnd == null || (this.__trackingDirection == -1 && value <= this.__trackingEnd) || (this.__trackingDirection == 1 && value >= this.__trackingEnd)) {
        this.__trackingEnd = value;
      }
    },


    /**
     * Converts the given position to a value.
     *
     * Does not respect single or page step.
     *
     * @param position {Integer} Position to use
     * @return {Integer} Resulting value (rounded)
     */
    _positionToValue : function(position)
    {
      // Reading available space
      var avail = this.__slidingSpace;

      // Protect undefined value (before initial resize) and division by zero
      if (avail == null || avail == 0) {
        return 0;
      }

      // Compute and limit percent
      var percent = position / avail;
      if (percent < 0) {
        percent = 0;
      } else if (percent > 1) {
        percent = 1;
      }

      // Compute range
      var range = this.getMaximum() - this.getMinimum();

      // Compute value
      return this.getMinimum() + Math.round(range * percent);
    },


    /**
     * Converts the given value to a position to place
     * the knob to.
     *
     * @param value {Integer} Value to use
     * @return {Integer} Computed position (rounded)
     */
    _valueToPosition : function(value)
    {
      // Reading available space
      var avail = this.__slidingSpace;
      if (avail == null) {
        return 0;
      }

      // Computing range
      var range = this.getMaximum() - this.getMinimum();

      // Protect division by zero
      if (range == 0) {
        return 0;
      }

      // Translating value to distance from minimum
      var value = value - this.getMinimum();

      // Compute and limit percent
      var percent = value / range;
      if (percent < 0) {
        percent = 0;
      } else if (percent > 1) {
        percent = 1;
      }

      // Compute position from available space and percent
      return Math.round(avail * percent);
    },


    /**
     * Updates the knob position following the currently configured
     * value. Useful on reflows where the dimensions of the slider
     * itself have been modified.
     *
     */
    _updateKnobPosition : function() {
      this._setKnobPosition(this._valueToPosition(this.getValue()));
    },


    /**
     * Moves the knob to the given position.
     *
     * @param position {Integer} Any valid position (needs to be
     *   greater or equal than zero)
     */
    _setKnobPosition : function(position)
    {
      // Use the DOM Element to prevent unnecessary layout recalculations
      var knob = this.getChildControl("knob");
      var dec = this.getDecorator();
      dec = qx.theme.manager.Decoration.getInstance().resolve(dec);
      var content = knob.getContentElement();
      if (this.__isHorizontal) {
        if (dec && dec.getPadding()) {
          position += dec.getPadding().left;
        }
        position += this.getPaddingLeft() || 0;
        content.setStyle("left", position+"px", true);
      } else {
        if (dec && dec.getPadding()) {
          position += dec.getPadding().top;
        }
        position += this.getPaddingTop() || 0;
        content.setStyle("top", position+"px", true);
      }
    },


    /**
     * Reconfigures the size of the knob depending on
     * the optionally defined {@link #knobFactor}.
     *
     */
    _updateKnobSize : function()
    {
      // Compute knob size
      var knobFactor = this.getKnobFactor();
      if (knobFactor == null) {
        return;
      }

      // Ignore when not rendered yet
      var avail = this.getInnerSize();
      if (avail == null) {
        return;
      }

      // Read size property
      if (this.__isHorizontal) {
        this.getChildControl("knob").setWidth(Math.round(knobFactor * avail.width));
      } else {
        this.getChildControl("knob").setHeight(Math.round(knobFactor * avail.height));
      }
    },





    /*
    ---------------------------------------------------------------------------
      SLIDE METHODS
    ---------------------------------------------------------------------------
    */

    /**
     * Slides backward to the minimum value
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    slideToBegin : function(duration) {
      this.slideTo(this.getMinimum(), duration);
    },


    /**
     * Slides forward to the maximum value
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    slideToEnd : function(duration) {
      this.slideTo(this.getMaximum(), duration);
    },


    /**
     * Slides forward (right or bottom depending on orientation)
     *
     */
    slideForward : function() {
      this.slideBy(this.getSingleStep());
    },


    /**
     * Slides backward (to left or top depending on orientation)
     *
     */
    slideBack : function() {
      this.slideBy(-this.getSingleStep());
    },


    /**
     * Slides a page forward (to right or bottom depending on orientation)
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    slidePageForward : function(duration) {
      this.slideBy(this.getPageStep(), duration);
    },


    /**
     * Slides a page backward (to left or top depending on orientation)
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    slidePageBack : function(duration) {
      this.slideBy(-this.getPageStep(), duration);
    },


    /**
     * Slides by the given offset.
     *
     * This method works with the value, not with the coordinate.
     *
     * @param offset {Integer} Offset to scroll by
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    slideBy : function(offset, duration) {
      this.slideTo(this.getValue() + offset, duration);
    },


    /**
     * Slides to the given value
     *
     * This method works with the value, not with the coordinate.
     *
     * @param value {Integer} Scroll to a value between the defined
     *   minimum and maximum.
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    slideTo : function(value, duration)
    {
      this.stopSlideAnimation();

      if (duration) {
        this.__animateTo(value, duration);
      } else {
        this.updatePosition(value);
      }
    },


    /**
     * Updates the position property considering the minimum and maximum values.
     * @param value {Number} The new position.
     */
    updatePosition : function(value) {
      this.setValue(this.__normalizeValue(value));
    },


    /**
     * In case a slide animation is currently running, it will be stopped.
     * If not, the method does nothing.
     */
    stopSlideAnimation : function() {
      if (this.__scrollAnimationframe) {
        this.__scrollAnimationframe.cancelSequence();
        this.__scrollAnimationframe = null;
      }
    },


    /**
     * Internal helper to normalize the given value concerning the minimum
     * and maximum value.
     * @param value {Number} The value to normalize.
     * @return {Number} The normalized value.
     */
    __normalizeValue : function(value) {
      // Bring into allowed range or fix to single step grid
      if (value < this.getMinimum()) {
        value = this.getMinimum();
      } else if (value > this.getMaximum()) {
        value = this.getMaximum();
      } else {
        value = this.getMinimum() + Math.round((value - this.getMinimum()) / this.getSingleStep()) * this.getSingleStep()
      }
      return value;
    },


    /**
     * Animation helper which takes care of the animated slide.
     * @param to {Number} The target value.
     * @param duration {Number} The time in milliseconds the slide to should take.
     */
    __animateTo : function(to, duration) {
      to = this.__normalizeValue(to);
      var from = this.getValue();

      this.__scrollAnimationframe = new qx.bom.AnimationFrame();

      this.__scrollAnimationframe.on("frame", function(timePassed) {
        this.setValue(parseInt(timePassed/duration * (to - from) + from));
      }, this);

      this.__scrollAnimationframe.on("end", function() {
        this.setValue(to);
        this.__scrollAnimationframe = null;
        this.fireEvent("slideAnimationEnd");
      }, this);

      this.__scrollAnimationframe.startSequence(duration);
    },


    /*
    ---------------------------------------------------------------------------
      PROPERTY APPLY ROUTINES
    ---------------------------------------------------------------------------
    */

    // property apply
    _applyOrientation : function(value, old)
    {
      var knob = this.getChildControl("knob");

      // Update private flag for faster access
      this.__isHorizontal = value === "horizontal";

      // Toggle states and knob layout
      if (this.__isHorizontal)
      {
        this.removeState("vertical");
        knob.removeState("vertical");

        this.addState("horizontal");
        knob.addState("horizontal");

        knob.setLayoutProperties({top:0, right:null, bottom:0});
      }
      else
      {
        this.removeState("horizontal");
        knob.removeState("horizontal");

        this.addState("vertical");
        knob.addState("vertical");

        knob.setLayoutProperties({right:0, bottom:null, left:0});
      }

      // Sync knob position
      this._updateKnobPosition();
    },


    // property apply
    _applyKnobFactor : function(value, old)
    {
      if (value != null)
      {
        this._updateKnobSize();
      }
      else
      {
        if (this.__isHorizontal) {
          this.getChildControl("knob").resetWidth();
        } else {
          this.getChildControl("knob").resetHeight();
        }
      }
    },


    // property apply
    _applyValue : function(value, old) {
      if (value != null) {
        this._updateKnobPosition();
        if (this.__dragMode) {
          this.__dragValue = [value,old];
        } else {
          this.fireEvent("changeValue", qx.event.type.Data, [value,old]);
        }
      } else {
        this.resetValue();
      }
    },


    /**
     * Helper for applyValue which fires the changeValue event.
     */
    _fireValue: function(){
      if (!this.__dragValue){
        return;
      }
      var tmp = this.__dragValue;
      this.__dragValue = null;
      this.fireEvent("changeValue", qx.event.type.Data, tmp);
    },


    // property apply
    _applyMinimum : function(value, old)
    {
      if (this.getValue() < value) {
        this.setValue(value);
      }

      this._updateKnobPosition();
    },


    // property apply
    _applyMaximum : function(value, old)
    {
      if (this.getValue() > value) {
        this.setValue(value);
      }

      this._updateKnobPosition();
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * The Canvas is an extended Basic layout.
 *
 * It is possible to position a widget relative to the right or bottom edge of
 * the available space. It further supports stretching between left and right
 * or top and bottom e.g. <code>left=20</code> and <code>right=20</code> would
 * keep a margin of 20 pixels to both edges. The Canvas layout has support for
 * percent dimensions and locations.
 *
 * *Features*
 *
 * * Pixel dimensions and locations
 * * Percent dimensions and locations
 * * Stretching between left+right and top+bottom
 * * Minimum and maximum dimensions
 * * Children are automatically shrunk to their minimum dimensions if not enough space is available
 * * Auto sizing (ignoring percent values)
 * * Margins (also negative ones)
 *
 * *Item Properties*
 *
 * <ul>
 * <li><strong>left</strong> <em>(Integer|String)</em>: The left coordinate in pixel or as a percent string e.g. <code>20</code> or <code>30%</code>.</li>
 * <li><strong>top</strong> <em>(Integer|String)</em>: The top coordinate in pixel or as a percent string e.g. <code>20</code> or <code>30%</code>.</li>
 * <li><strong>right</strong> <em>(Integer|String)</em>: The right coordinate in pixel or as a percent string e.g. <code>20</code> or <code>30%</code>.</li>
 * <li><strong>bottom</strong> <em>(Integer|String)</em>: The bottom coordinate in pixel or as a percent string e.g. <code>20</code> or <code>30%</code>.</li>
 * <li><strong>edge</strong> <em>(Integer|String)</em>: The coordinate in pixels or as a percent string to be used for all four edges.
 * <li><strong>width</strong> <em>(String)</em>: A percent width e.g. <code>40%</code>.</li>
 * <li><strong>height</strong> <em>(String)</em>: A percent height e.g. <code>60%</code>.</li>
 * </ul>
 *
 * *Notes*
 *
 * <ul>
 * <li>Stretching (<code>left</code>-><code>right</code> or <code>top</code>-><code>bottom</code>)
 *   has a higher priority than the preferred dimensions</li>
 * <li>Stretching has a lower priority than the min/max dimensions.</li>
 * <li>Percent values have no influence on the size hint of the layout.</li>
 * </ul>
 *
 * *Example*
 *
 * Here is a little example of how to use the canvas layout.
 *
 * <pre class="javascript">
 * var container = new qx.ui.container.Composite(new qx.ui.layout.Canvas());
 *
 * // simple positioning
 * container.add(new qx.ui.core.Widget(), {top: 10, left: 10});
 *
 * // stretch vertically with 10 pixel distance to the parent's top
 * // and bottom border
 * container.add(new qx.ui.core.Widget(), {top: 10, left: 10, bottom: 10});
 *
 * // percent positioning and size
 * container.add(new qx.ui.core.Widget(), {left: "50%", top: "50%", width: "25%", height: "40%"});
 * </pre>
 *
 * *External Documentation*
 *
 * <a href='http://manual.qooxdoo.org/${qxversion}/pages/layout/canvas.html'>
 * Extended documentation</a> and links to demos of this layout in the qooxdoo manual.
 */
qx.Class.define("qx.ui.layout.Canvas",
{
  extend : qx.ui.layout.Abstract,




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties : {

    /**
     * If desktop mode is active, the children's minimum sizes are ignored
     * by the layout calculation. This is necessary to prevent the desktop
     * from growing if e.g. a window is moved beyond the edge of the desktop
     */
    desktop :
    {
      check : "Boolean",
      init: false
    }
  },



  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
    ---------------------------------------------------------------------------
      LAYOUT INTERFACE
    ---------------------------------------------------------------------------
    */

    // overridden
    verifyLayoutProperty : qx.core.Environment.select("qx.debug",
    {
      "true" : function(item, name, value)
      {
        var layoutProperties =
        {
          top : 1,
          left : 1,
          bottom : 1,
          right : 1,
          width : 1,
          height : 1,
          edge : 1
        };

        this.assert(layoutProperties[name] == 1, "The property '"+name+"' is not supported by the Canvas layout!");

        if (name =="width" || name == "height")
        {
          this.assertMatch(value, qx.ui.layout.Util.PERCENT_VALUE);
        }
        else
        {
          if (typeof value === "number") {
            this.assertInteger(value);
          } else if (qx.lang.Type.isString(value)) {
            this.assertMatch(value, qx.ui.layout.Util.PERCENT_VALUE);
          } else {
            this.fail(
              "Bad format of layout property '" + name + "': " + value +
              ". The value must be either an integer or an percent string."
            );
          }
        }
      },

      "false" : null
    }),


    // overridden
    renderLayout : function(availWidth, availHeight, padding)
    {
      var children = this._getLayoutChildren();

      var child, size, props;
      var left, top, right, bottom, width, height;
      var marginTop, marginRight, marginBottom, marginLeft;

      for (var i=0, l=children.length; i<l; i++)
      {
        child = children[i];
        size = child.getSizeHint();
        props = child.getLayoutProperties();

        // Cache margins
        marginTop = child.getMarginTop();
        marginRight = child.getMarginRight();
        marginBottom = child.getMarginBottom();
        marginLeft = child.getMarginLeft();



        // **************************************
        //   Processing location
        // **************************************

        left = props.left != null ? props.left : props.edge;
        if (qx.lang.Type.isString(left)) {
          left = Math.round(parseFloat(left) * availWidth / 100);
        }

        right = props.right != null ? props.right : props.edge;
        if (qx.lang.Type.isString(right)) {
          right = Math.round(parseFloat(right) * availWidth / 100);
        }

        top = props.top != null ? props.top : props.edge;
        if (qx.lang.Type.isString(top)) {
          top = Math.round(parseFloat(top) * availHeight / 100);
        }

        bottom = props.bottom != null ? props.bottom : props.edge;
        if (qx.lang.Type.isString(bottom)) {
          bottom = Math.round(parseFloat(bottom) * availHeight / 100);
        }



        // **************************************
        //   Processing dimension
        // **************************************

        // Stretching has higher priority than dimension data
        if (left != null && right != null)
        {
          width = availWidth - left - right - marginLeft - marginRight;

          // Limit computed value
          if (width < size.minWidth) {
            width = size.minWidth;
          } else if (width > size.maxWidth) {
            width = size.maxWidth;
          }

          // Add margin
          left += marginLeft;
        }
        else
        {
          // Layout data has higher priority than data from size hint
          width = props.width;

          if (width == null)
          {
            width = size.width;
          }
          else
          {
            width = Math.round(parseFloat(width) * availWidth / 100);

            // Limit computed value
            if (width < size.minWidth) {
              width = size.minWidth;
            } else if (width > size.maxWidth) {
              width = size.maxWidth;
            }
          }

          if (right != null) {
            left = availWidth - width - right - marginRight - marginLeft;
          } else if (left == null) {
            left = marginLeft;
          } else {
            left += marginLeft;
          }
        }

        // Stretching has higher priority than dimension data
        if (top != null && bottom != null)
        {
          height = availHeight - top - bottom - marginTop - marginBottom;

          // Limit computed value
          if (height < size.minHeight) {
            height = size.minHeight;
          } else if (height > size.maxHeight) {
            height = size.maxHeight;
          }

          // Add margin
          top += marginTop;
        }
        else
        {
          // Layout data has higher priority than data from size hint
          height = props.height;

          if (height == null)
          {
            height = size.height;
          }
          else
          {
            height = Math.round(parseFloat(height) * availHeight / 100);

            // Limit computed value
            if (height < size.minHeight) {
              height = size.minHeight;
            } else if (height > size.maxHeight) {
              height = size.maxHeight;
            }
          }

          if (bottom != null) {
            top = availHeight - height - bottom - marginBottom - marginTop;
          } else if (top == null) {
            top = marginTop;
          } else {
            top += marginTop;
          }
        }

        left += padding.left;
        top += padding.top;

        // Apply layout
        child.renderLayout(left, top, width, height);
      }
    },


    // overridden
    _computeSizeHint : function()
    {
      var neededWidth=0, neededMinWidth=0;
      var neededHeight=0, neededMinHeight=0;

      var width, minWidth;
      var height, minHeight;

      var children = this._getLayoutChildren();
      var child, props, hint;
      var desktop = this.isDesktop();

      var left, top, right, bottom;

      for (var i=0,l=children.length; i<l; i++)
      {
        child = children[i];
        props = child.getLayoutProperties();
        hint = child.getSizeHint();


        // Cache margins
        var marginX = child.getMarginLeft() + child.getMarginRight();
        var marginY = child.getMarginTop() + child.getMarginBottom();


        // Compute width
        width = hint.width+marginX;
        minWidth = hint.minWidth+marginX;

        left = props.left != null ? props.left : props.edge;
        if (left && typeof left === "number")
        {
          width += left;
          minWidth += left;
        }

        right = props.right != null ? props.right : props.edge;
        if (right && typeof right === "number")
        {
          width += right;
          minWidth += right;
        }

        neededWidth = Math.max(neededWidth, width);
        neededMinWidth = desktop ? 0 : Math.max(neededMinWidth, minWidth);


        // Compute height
        height = hint.height+marginY;
        minHeight = hint.minHeight+marginY;

        top = props.top != null ? props.top : props.edge;
        if (top && typeof top === "number")
        {
          height += top;
          minHeight += top;
        }

        bottom = props.bottom != null ? props.bottom : props.edge;
        if (bottom && typeof bottom === "number")
        {
          height += bottom;
          minHeight += bottom;
        }

        neededHeight = Math.max(neededHeight, height);
        neededMinHeight = desktop ? 0 : Math.max(neededMinHeight, minHeight);
      }

      return {
        width : neededWidth,
        minWidth : neededMinWidth,
        height : neededHeight,
        minHeight : neededMinHeight
      };
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's left-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * Minimal modified version of the {@link qx.ui.form.Slider} to be
 * used by {@link qx.ui.core.scroll.ScrollBar}.
 *
 * @internal
 */
qx.Class.define("qx.ui.core.scroll.ScrollSlider",
{
  extend : qx.ui.form.Slider,

  // overridden
  construct : function(orientation)
  {
    this.base(arguments, orientation);

    // Remove mousewheel/keypress events
    this.removeListener("keypress", this._onKeyPress);
    this.removeListener("mousewheel", this._onMouseWheel);
  },


  members : {
    // overridden
    getSizeHint : function(compute) {
      // get the original size hint
      var hint = this.base(arguments);
      // set the width or height to 0 depending on the orientation.
      // this is necessary to prevent the ScrollSlider to change the size
      // hint of its parent, which can cause errors on outer flex layouts
      // [BUG #3279]
      if (this.getOrientation() === "horizontal") {
        hint.width = 0;
      } else {
        hint.height = 0;
      }
      return hint;
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2011 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Mixin holding the handler for the two axis mouse wheel scrolling. Please
 * keep in mind that the including widget has to have the scroll bars
 * implemented as child controls named <code>scrollbar-x</code> and
 * <code>scrollbar-y</code> to get the handler working. Also, you have to
 * attach the listener yourself.
 */
qx.Mixin.define("qx.ui.core.scroll.MWheelHandling",
{
  members :
  {
    /**
     * Mouse wheel event handler
     *
     * @param e {qx.event.type.Mouse} Mouse event
     */
    _onMouseWheel : function(e)
    {
      var showX = this._isChildControlVisible("scrollbar-x");
      var showY = this._isChildControlVisible("scrollbar-y");

      var scrollbarY = showY ? this.getChildControl("scrollbar-y", true) : null;
      var scrollbarX = showX ? this.getChildControl("scrollbar-x", true) : null;

      var deltaY = e.getWheelDelta("y");
      var deltaX = e.getWheelDelta("x");

      var endY = !showY;
      var endX = !showX;

      // y case
      if (scrollbarY) {
        if (qx.event.handler.MouseEmulation.ON) {
          scrollbarY.scrollBy(parseInt(deltaY));
        } else {
          var steps = parseInt(deltaY);

          if (steps !== 0) {
            scrollbarY.scrollBySteps(steps);
          }
        }


        var position = scrollbarY.getPosition();
        var max = scrollbarY.getMaximum();

        // pass the event to the parent if the scrollbar is at an edge
        if (steps < 0 && position <= 0 || steps > 0 && position >= max) {
          endY = true;
        }
      }

      // x case
      if (scrollbarX) {
        if (qx.event.handler.MouseEmulation.ON) {
          scrollbarX.scrollBySteps(deltaX);
        } else {
          var steps = parseInt(deltaX);

          if (steps !== 0) {
            scrollbarX.scrollBySteps(steps);
          }
        }

        var position = scrollbarX.getPosition();
        var max = scrollbarX.getMaximum();
        // pass the event to the parent if the scrollbar is at an edge
        if (steps < 0 && position <= 0 || steps > 0 && position >= max) {
          endX = true;
        }
      }

      // pass the event to the parent if both scrollbars are at the end
      if ((!endY && deltaX === 0) ||
          (!endX && deltaY === 0) ||
          ((!endX || !endY ) && deltaX !== 0 && deltaY !== 0)) {
        // Stop bubbling and native event only if a scrollbar is visible
        e.stop();
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's left-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * The ScrollArea provides a container widget with on demand scroll bars
 * if the content size exceeds the size of the container.
 *
 * @childControl pane {qx.ui.core.scroll.ScrollPane} pane which holds the content to scroll
 * @childControl scrollbar-x {qx.ui.core.scroll.ScrollBar?qx.ui.core.scroll.NativeScrollBar} horizontal scrollbar
 * @childControl scrollbar-y {qx.ui.core.scroll.ScrollBar?qx.ui.core.scroll.NativeScrollBar} vertical scrollbar
 * @childControl corner {qx.ui.core.Widget} corner where no scrollbar is shown
 */
qx.Class.define("qx.ui.core.scroll.AbstractScrollArea",
{
  extend : qx.ui.core.Widget,
  include : [
    qx.ui.core.scroll.MScrollBarFactory,
    qx.ui.core.scroll.MWheelHandling,
    qx.ui.core.MDragDropScrolling
  ],
  type : "abstract",


  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

  statics :
  {
    /**
     * The default width which is used for the width of the scroll bar if
     * overlaid.
     */
    DEFAULT_SCROLLBAR_WIDTH : 14
  },



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    if (qx.core.Environment.get("os.scrollBarOverlayed")) {
      // use a plain canvas to overlay the scroll bars
      this._setLayout(new qx.ui.layout.Canvas());
    } else {
      // Create 'fixed' grid layout
      var grid = new qx.ui.layout.Grid();
      grid.setColumnFlex(0, 1);
      grid.setRowFlex(0, 1);
      this._setLayout(grid);
    }

    // Mousewheel listener to scroll vertically
    this.addListener("mousewheel", this._onMouseWheel, this);
  },


  events : {
    /** Fired as soon as the scroll animation in X direction ends. */
    scrollAnimationXEnd: 'qx.event.type.Event',

    /** Fired as soon as the scroll animation in X direction ends. */
    scrollAnimationYEnd: 'qx.event.type.Event'
  },



  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "scrollarea"
    },


    // overridden
    width :
    {
      refine : true,
      init : 100
    },


    // overridden
    height :
    {
      refine : true,
      init : 200
    },


    /**
     * The policy, when the horizontal scrollbar should be shown.
     * <ul>
     *   <li><b>auto</b>: Show scrollbar on demand</li>
     *   <li><b>on</b>: Always show the scrollbar</li>
     *   <li><b>off</b>: Never show the scrollbar</li>
     * </ul>
     */
    scrollbarX :
    {
      check : ["auto", "on", "off"],
      init : "auto",
      themeable : true,
      apply : "_computeScrollbars"
    },


    /**
     * The policy, when the horizontal scrollbar should be shown.
     * <ul>
     *   <li><b>auto</b>: Show scrollbar on demand</li>
     *   <li><b>on</b>: Always show the scrollbar</li>
     *   <li><b>off</b>: Never show the scrollbar</li>
     * </ul>
     */
    scrollbarY :
    {
      check : ["auto", "on", "off"],
      init : "auto",
      themeable : true,
      apply : "_computeScrollbars"
    },


    /**
     * Group property, to set the overflow of both scroll bars.
     */
    scrollbar : {
      group : [ "scrollbarX", "scrollbarY" ]
    }
  },






  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
    ---------------------------------------------------------------------------
      CHILD CONTROL SUPPORT
    ---------------------------------------------------------------------------
    */

    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "pane":
          control = new qx.ui.core.scroll.ScrollPane();

          control.addListener("update", this._computeScrollbars, this);
          control.addListener("scrollX", this._onScrollPaneX, this);
          control.addListener("scrollY", this._onScrollPaneY, this);

          if (qx.core.Environment.get("os.scrollBarOverlayed")) {
            this._add(control, {edge: 0});
          } else {
            this._add(control, {row: 0, column: 0});
          }
          break;


        case "scrollbar-x":
          control = this._createScrollBar("horizontal");
          control.setMinWidth(0);

          control.exclude();
          control.addListener("scroll", this._onScrollBarX, this);
          control.addListener("changeVisibility", this._onChangeScrollbarXVisibility, this);
          control.addListener("scrollAnimationEnd", this._onScrollAnimationEnd.bind(this, "X"));

          if (qx.core.Environment.get("os.scrollBarOverlayed")) {
            control.setMinHeight(qx.ui.core.scroll.AbstractScrollArea.DEFAULT_SCROLLBAR_WIDTH);
            this._add(control, {bottom: 0, right: 0, left: 0});
          } else {
            this._add(control, {row: 1, column: 0});
          }
          break;


        case "scrollbar-y":
          control = this._createScrollBar("vertical");
          control.setMinHeight(0);

          control.exclude();
          control.addListener("scroll", this._onScrollBarY, this);
          control.addListener("changeVisibility", this._onChangeScrollbarYVisibility, this);
          control.addListener("scrollAnimationEnd", this._onScrollAnimationEnd.bind(this, "Y"));

          if (qx.core.Environment.get("os.scrollBarOverlayed")) {
            control.setMinWidth(qx.ui.core.scroll.AbstractScrollArea.DEFAULT_SCROLLBAR_WIDTH);
            this._add(control, {right: 0, bottom: 0, top: 0});
          } else {
            this._add(control, {row: 0, column: 1});
          }
          break;


        case "corner":
          control = new qx.ui.core.Widget();
          control.setWidth(0);
          control.setHeight(0);
          control.exclude();

          if (!qx.core.Environment.get("os.scrollBarOverlayed")) {
            // only add for non overlayed scroll bars
            this._add(control, {row: 1, column: 1});
          }
          break;
      }

      return control || this.base(arguments, id);
    },




    /*
    ---------------------------------------------------------------------------
      PANE SIZE
    ---------------------------------------------------------------------------
    */

    /**
     * Returns the boundaries of the pane.
     *
     * @return {Map} The pane boundaries.
     */
    getPaneSize : function() {
      return this.getChildControl("pane").getInnerSize();
    },






    /*
    ---------------------------------------------------------------------------
      ITEM LOCATION SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * Returns the top offset of the given item in relation to the
     * inner height of this widget.
     *
     * @param item {qx.ui.core.Widget} Item to query
     * @return {Integer} Top offset
     */
    getItemTop : function(item) {
      return this.getChildControl("pane").getItemTop(item);
    },


    /**
     * Returns the top offset of the end of the given item in relation to the
     * inner height of this widget.
     *
     * @param item {qx.ui.core.Widget} Item to query
     * @return {Integer} Top offset
     */
    getItemBottom : function(item) {
      return this.getChildControl("pane").getItemBottom(item);
    },


    /**
     * Returns the left offset of the given item in relation to the
     * inner width of this widget.
     *
     * @param item {qx.ui.core.Widget} Item to query
     * @return {Integer} Top offset
     */
    getItemLeft : function(item) {
      return this.getChildControl("pane").getItemLeft(item);
    },


    /**
     * Returns the left offset of the end of the given item in relation to the
     * inner width of this widget.
     *
     * @param item {qx.ui.core.Widget} Item to query
     * @return {Integer} Right offset
     */
    getItemRight : function(item) {
      return this.getChildControl("pane").getItemRight(item);
    },





    /*
    ---------------------------------------------------------------------------
      SCROLL SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * Scrolls the element's content to the given left coordinate
     *
     * @param value {Integer} The vertical position to scroll to.
     * @param duration {Number?} The time in milliseconds the scroll to should take.
     */
    scrollToX : function(value, duration) {
      // First flush queue before scroll
      qx.ui.core.queue.Manager.flush();

      this.getChildControl("scrollbar-x").scrollTo(value, duration);
    },


    /**
     * Scrolls the element's content by the given left offset
     *
     * @param value {Integer} The vertical position to scroll to.
     * @param duration {Number?} The time in milliseconds the scroll to should take.
     */
    scrollByX : function(value, duration) {
      // First flush queue before scroll
      qx.ui.core.queue.Manager.flush();

      this.getChildControl("scrollbar-x").scrollBy(value, duration);
    },


    /**
     * Returns the scroll left position of the content
     *
     * @return {Integer} Horizontal scroll position
     */
    getScrollX : function()
    {
      var scrollbar = this.getChildControl("scrollbar-x", true);
      return scrollbar ? scrollbar.getPosition() : 0;
    },


    /**
     * Scrolls the element's content to the given top coordinate
     *
     * @param value {Integer} The horizontal position to scroll to.
     * @param duration {Number?} The time in milliseconds the scroll to should take.
     */
    scrollToY : function(value, duration) {
      // First flush queue before scroll
      qx.ui.core.queue.Manager.flush();

      this.getChildControl("scrollbar-y").scrollTo(value, duration);
    },


    /**
     * Scrolls the element's content by the given top offset
     *
     * @param value {Integer} The horizontal position to scroll to.
     * @param duration {Number?} The time in milliseconds the scroll to should take.
     */
    scrollByY : function(value, duration) {
      // First flush queue before scroll
      qx.ui.core.queue.Manager.flush();

      this.getChildControl("scrollbar-y").scrollBy(value, duration);
    },


    /**
     * Returns the scroll top position of the content
     *
     * @return {Integer} Vertical scroll position
     */
    getScrollY : function()
    {
      var scrollbar = this.getChildControl("scrollbar-y", true);
      return scrollbar ? scrollbar.getPosition() : 0;
    },


    /**
     * In case a scroll animation is currently running in X direction,
     * it will be stopped. If not, the method does nothing.
     */
    stopScrollAnimationX : function() {
      var scrollbar = this.getChildControl("scrollbar-x", true);
      if (scrollbar) {
        scrollbar.stopScrollAnimation();
      }
    },


    /**
     * In case a scroll animation is currently running in X direction,
     * it will be stopped. If not, the method does nothing.
     */
    stopScrollAnimationY : function() {
      var scrollbar = this.getChildControl("scrollbar-y", true);
      if (scrollbar) {
        scrollbar.stopScrollAnimation();
      }
    },



    /*
    ---------------------------------------------------------------------------
      EVENT LISTENERS
    ---------------------------------------------------------------------------
    */
    /**
     * Event handler for the scroll animation end event for both scroll bars.
     *
     * @param direction {String} Either "X" or "Y".
     */
    _onScrollAnimationEnd : function(direction) {
      this.fireEvent("scrollAnimation" + direction + "End");
    },

    /**
     * Event handler for the scroll event of the horizontal scrollbar
     *
     * @param e {qx.event.type.Data} The scroll event object
     */
    _onScrollBarX : function(e) {
      this.getChildControl("pane").scrollToX(e.getData());
    },


    /**
     * Event handler for the scroll event of the vertical scrollbar
     *
     * @param e {qx.event.type.Data} The scroll event object
     */
    _onScrollBarY : function(e) {
      this.getChildControl("pane").scrollToY(e.getData());
    },


    /**
     * Event handler for the horizontal scroll event of the pane
     *
     * @param e {qx.event.type.Data} The scroll event object
     */
    _onScrollPaneX : function(e) {
      var scrollbar = this.getChildControl("scrollbar-x");
      if (scrollbar) {
        scrollbar.updatePosition(e.getData());
      }
    },


    /**
     * Event handler for the vertical scroll event of the pane
     *
     * @param e {qx.event.type.Data} The scroll event object
     */
    _onScrollPaneY : function(e) {
      var scrollbar = this.getChildControl("scrollbar-y");
      if (scrollbar) {
        scrollbar.updatePosition(e.getData());
      }
    },


    /**
     * Event handler for visibility changes of horizontal scrollbar.
     *
     * @param e {qx.event.type.Event} Property change event
     */
    _onChangeScrollbarXVisibility : function(e)
    {
      var showX = this._isChildControlVisible("scrollbar-x");
      var showY = this._isChildControlVisible("scrollbar-y");

      if (!showX) {
        this.scrollToX(0);
      }

      showX && showY ? this._showChildControl("corner") : this._excludeChildControl("corner");
    },


    /**
     * Event handler for visibility changes of horizontal scrollbar.
     *
     * @param e {qx.event.type.Event} Property change event
     */
    _onChangeScrollbarYVisibility : function(e)
    {
      var showX = this._isChildControlVisible("scrollbar-x");
      var showY = this._isChildControlVisible("scrollbar-y");

      if (!showY) {
        this.scrollToY(0);
      }

      showX && showY ? this._showChildControl("corner") : this._excludeChildControl("corner");
    },




    /*
    ---------------------------------------------------------------------------
      HELPER METHODS
    ---------------------------------------------------------------------------
    */

    /**
     * Computes the visibility state for scrollbars.
     *
     */
    _computeScrollbars : function()
    {
      var pane = this.getChildControl("pane");
      var content = pane.getChildren()[0];
      if (!content)
      {
        this._excludeChildControl("scrollbar-x");
        this._excludeChildControl("scrollbar-y");
        return;
      }

      var innerSize = this.getInnerSize();
      var paneSize = pane.getInnerSize();
      var scrollSize = pane.getScrollSize();

      // if the widget has not yet been rendered, return and try again in the
      // resize event
      if (!paneSize || !scrollSize) {
        return;
      }

      var scrollbarX = this.getScrollbarX();
      var scrollbarY = this.getScrollbarY();

      if (scrollbarX === "auto" && scrollbarY === "auto")
      {
        // Check if the container is big enough to show
        // the full content.
        var showX = scrollSize.width > innerSize.width;
        var showY = scrollSize.height > innerSize.height;

        // Dependency check
        // We need a special intelligence here when only one
        // of the autosized axis requires a scrollbar
        // This scrollbar may then influence the need
        // for the other one as well.
        if ((showX || showY) && !(showX && showY))
        {
          if (showX) {
            showY = scrollSize.height > paneSize.height;
          } else if (showY) {
            showX = scrollSize.width > paneSize.width;
          }
        }
      }
      else
      {
        var showX = scrollbarX === "on";
        var showY = scrollbarY === "on";

        // Check auto values afterwards with already
        // corrected client dimensions
        if (scrollSize.width > (showX ? paneSize.width : innerSize.width) && scrollbarX === "auto") {
          showX = true;
        }

        if (scrollSize.height > (showX ? paneSize.height : innerSize.height) && scrollbarY === "auto") {
          showY = true;
        }
      }

      // Update scrollbars
      if (showX)
      {
        var barX = this.getChildControl("scrollbar-x");

        barX.show();
        barX.setMaximum(Math.max(0, scrollSize.width - paneSize.width));
        barX.setKnobFactor((scrollSize.width === 0) ? 0 : paneSize.width / scrollSize.width);
      }
      else
      {
        this._excludeChildControl("scrollbar-x");
      }

      if (showY)
      {
        var barY = this.getChildControl("scrollbar-y");

        barY.show();
        barY.setMaximum(Math.max(0, scrollSize.height - paneSize.height));
        barY.setKnobFactor((scrollSize.height === 0) ? 0 : paneSize.height / scrollSize.height);
      }
      else
      {
        this._excludeChildControl("scrollbar-y");
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Martin Wittemann (martinwittemann)
     * Christian Hagendorn (chris_schmidt)

************************************************************************ */

/**
 * A list of items. Displays an automatically scrolling list for all
 * added {@link qx.ui.form.ListItem} instances. Supports various
 * selection options: single, multi, ...
 */
qx.Class.define("qx.ui.form.List",
{
  extend : qx.ui.core.scroll.AbstractScrollArea,
  implement : [
    qx.ui.core.IMultiSelection,
    qx.ui.form.IForm,
    qx.ui.form.IModelSelection
  ],
  include : [
    qx.ui.core.MRemoteChildrenHandling,
    qx.ui.core.MMultiSelectionHandling,
    qx.ui.form.MForm,
    qx.ui.form.MModelSelection
  ],


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param horizontal {Boolean?false} Whether the list should be horizontal.
   */
  construct : function(horizontal)
  {
    this.base(arguments);

    // Create content
    this.__content = this._createListItemContainer();

    // Used to fire item add/remove events
    this.__content.addListener("addChildWidget", this._onAddChild, this);
    this.__content.addListener("removeChildWidget", this._onRemoveChild, this);

    // Add to scrollpane
    this.getChildControl("pane").add(this.__content);

    // Apply orientation
    if (horizontal) {
      this.setOrientation("horizontal");
    } else {
      this.initOrientation();
    }

    // Add keypress listener
    this.addListener("keypress", this._onKeyPress);
    this.addListener("keyinput", this._onKeyInput);

    // initialize the search string
    this.__pressedString = "";
  },


  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */


  events :
  {
    /**
     * This event is fired after a list item was added to the list. The
     * {@link qx.event.type.Data#getData} method of the event returns the
     * added item.
     */
    addItem : "qx.event.type.Data",

    /**
     * This event is fired after a list item has been removed from the list.
     * The {@link qx.event.type.Data#getData} method of the event returns the
     * removed item.
     */
    removeItem : "qx.event.type.Data"
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */


  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "list"
    },

    // overridden
    focusable :
    {
      refine : true,
      init : true
    },

    /**
     * Whether the list should be rendered horizontal or vertical.
     */
    orientation :
    {
      check : ["horizontal", "vertical"],
      init : "vertical",
      apply : "_applyOrientation"
    },

    /** Spacing between the items */
    spacing :
    {
      check : "Integer",
      init : 0,
      apply : "_applySpacing",
      themeable : true
    },

    /** Controls whether the inline-find feature is activated or not */
    enableInlineFind :
    {
      check : "Boolean",
      init : true
    }
  },


  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */


  members :
  {
    __pressedString : null,
    __lastKeyPress : null,

    /** @type {qx.ui.core.Widget} The children container */
    __content : null,

    /** @type {Class} Pointer to the selection manager to use */
    SELECTION_MANAGER : qx.ui.core.selection.ScrollArea,


    /*
    ---------------------------------------------------------------------------
      WIDGET API
    ---------------------------------------------------------------------------
    */


    // overridden
    getChildrenContainer : function() {
      return this.__content;
    },

    /**
     * Handle child widget adds on the content pane
     *
     * @param e {qx.event.type.Data} the event instance
     */
    _onAddChild : function(e) {
      this.fireDataEvent("addItem", e.getData());
    },

    /**
     * Handle child widget removes on the content pane
     *
     * @param e {qx.event.type.Data} the event instance
     */
    _onRemoveChild : function(e) {
      this.fireDataEvent("removeItem", e.getData());
    },


    /*
    ---------------------------------------------------------------------------
      PUBLIC API
    ---------------------------------------------------------------------------
    */


    /**
     * Used to route external <code>keypress</code> events to the list
     * handling (in fact the manager of the list)
     *
     * @param e {qx.event.type.KeySequence} KeyPress event
     */
    handleKeyPress : function(e)
    {
      if (!this._onKeyPress(e)) {
        this._getManager().handleKeyPress(e);
      }
    },



    /*
    ---------------------------------------------------------------------------
      PROTECTED API
    ---------------------------------------------------------------------------
    */

    /**
     * This container holds the list item widgets.
     *
     * @return {qx.ui.container.Composite} Container for the list item widgets
     */
    _createListItemContainer : function() {
      return new qx.ui.container.Composite;
    },

    /*
    ---------------------------------------------------------------------------
      PROPERTY APPLY ROUTINES
    ---------------------------------------------------------------------------
    */


    // property apply
    _applyOrientation : function(value, old)
    {
      // Create new layout
      var horizontal = value === "horizontal";
      var layout = horizontal ? new qx.ui.layout.HBox() : new qx.ui.layout.VBox();

      // Configure content
      var content = this.__content;
      content.setLayout(layout);
      content.setAllowGrowX(!horizontal);
      content.setAllowGrowY(horizontal);

      // Configure spacing
      this._applySpacing(this.getSpacing());
    },

    // property apply
    _applySpacing : function(value, old) {
      this.__content.getLayout().setSpacing(value);
    },


    /*
    ---------------------------------------------------------------------------
      EVENT HANDLER
    ---------------------------------------------------------------------------
    */


    /**
     * Event listener for <code>keypress</code> events.
     *
     * @param e {qx.event.type.KeySequence} KeyPress event
     * @return {Boolean} Whether the event was processed
     */
    _onKeyPress : function(e)
    {
      // Execute action on press <ENTER>
      if (e.getKeyIdentifier() == "Enter" && !e.isAltPressed())
      {
        var items = this.getSelection();
        for (var i=0; i<items.length; i++) {
          items[i].fireEvent("action");
        }

        return true;
      }

      return false;
    },


    /*
    ---------------------------------------------------------------------------
      FIND SUPPORT
    ---------------------------------------------------------------------------
    */


    /**
     * Handles the inline find - if enabled
     *
     * @param e {qx.event.type.KeyInput} key input event
     */
    _onKeyInput : function(e)
    {
      // do nothing if the find is disabled
      if (!this.getEnableInlineFind()) {
        return;
      }

      // Only useful in single or one selection mode
      var mode = this.getSelectionMode();
      if (!(mode === "single" || mode === "one")) {
        return;
      }

      // Reset string after a second of non pressed key
      if (((new Date).valueOf() - this.__lastKeyPress) > 1000) {
        this.__pressedString = "";
      }

      // Combine keys the user pressed to a string
      this.__pressedString += e.getChar();

      // Find matching item
      var matchedItem = this.findItemByLabelFuzzy(this.__pressedString);

      // if an item was found, select it
      if (matchedItem) {
        this.setSelection([matchedItem]);
      }

      // Store timestamp
      this.__lastKeyPress = (new Date).valueOf();
    },

    /**
     * Takes the given string and tries to find a ListItem
     * which starts with this string. The search is not case sensitive and the
     * first found ListItem will be returned. If there could not be found any
     * qualifying list item, null will be returned.
     *
     * @param search {String} The text with which the label of the ListItem should start with
     * @return {qx.ui.form.ListItem} The found ListItem or null
     */
    findItemByLabelFuzzy : function(search)
    {
      // lower case search text
      search = search.toLowerCase();

      // get all items of the list
      var items = this.getChildren();

      // go threw all items
      for (var i=0, l=items.length; i<l; i++)
      {
        // get the label of the current item
        var currentLabel = items[i].getLabel();

        // if the label fits with the search text (ignore case, begins with)
        if (currentLabel && currentLabel.toLowerCase().indexOf(search) == 0)
        {
          // just return the first found element
          return items[i];
        }
      }

      // if no element was found, return null
      return null;
    },

    /**
     * Find an item by its {@link qx.ui.basic.Atom#getLabel}.
     *
     * @param search {String} A label or any item
     * @param ignoreCase {Boolean?true} description
     * @return {qx.ui.form.ListItem} The found ListItem or null
     */
    findItem : function(search, ignoreCase)
    {
      // lowercase search
      if (ignoreCase !== false) {
        search = search.toLowerCase();
      };

      // get all items of the list
      var items = this.getChildren();
      var item;

      // go through all items
      for (var i=0, l=items.length; i<l; i++)
      {
        item = items[i];

        // get the content of the label; text content when rich
        var label;

        if (item.isRich()) {
          var control = item.getChildControl("label", true);
          if (control) {
            var labelNode = control.getContentElement().getDomElement();
            if (labelNode) {
              label = qx.bom.element.Attribute.get(labelNode, "text");
            }
          }

        } else {
          label = item.getLabel();
        }

        if (label != null) {
          if (label.translate) {
            label = label.translate();
          }
          if (ignoreCase !== false) {
            label = label.toLowerCase();
          }

          if (label.toString() == search.toString()) {
            return item;
          }
        }
      }

      return null;
    }
  },


  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function() {
    this._disposeObjects("__content");
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)
     * Sebastian Werner (wpbasti)
     * Jonathan Weiß (jonathan_rass)
     * Christian Hagendorn (chris_schmidt)

************************************************************************ */

/**
 * A form widget which allows a single selection. Looks somewhat like
 * a normal button, but opens a list of items to select when clicking on it.
 *
 * Keep in mind that the SelectBox widget has always a selected item (due to the
 * single selection mode). Right after adding the first item a <code>changeSelection</code>
 * event is fired.
 *
 * <pre class='javascript'>
 * var selectBox = new qx.ui.form.SelectBox();
 *
 * selectBox.addListener("changeSelection", function(e) {
 *   // ...
 * });
 *
 * // now the 'changeSelection' event is fired
 * selectBox.add(new qx.ui.form.ListItem("Item 1"));
 * </pre>
 *
 * @childControl spacer {qx.ui.core.Spacer} flexible spacer widget
 * @childControl atom {qx.ui.basic.Atom} shows the text and icon of the content
 * @childControl arrow {qx.ui.basic.Image} shows the arrow to open the popup
 */
qx.Class.define("qx.ui.form.SelectBox",
{
  extend : qx.ui.form.AbstractSelectBox,
  implement : [
    qx.ui.core.ISingleSelection,
    qx.ui.form.IModelSelection
  ],
  include : [qx.ui.core.MSingleSelectionHandling, qx.ui.form.MModelSelection],


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */


  construct : function()
  {
    this.base(arguments);

    this._createChildControl("atom");
    this._createChildControl("spacer");
    this._createChildControl("arrow");

    // Register listener
    this.addListener("mouseover", this._onMouseOver, this);
    this.addListener("mouseout", this._onMouseOut, this);
    this.addListener("click", this._onClick, this);
    if (!(qx.event.handler.MouseEmulation.ON)) {
      this.addListener("mousewheel", this._onMouseWheel, this);
    }

    this.addListener("keyinput", this._onKeyInput, this);
    this.addListener("changeSelection", this.__onChangeSelection, this);
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */


  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "selectbox"
    }
  },


  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */


  members :
  {
    /** @type {qx.ui.form.ListItem} instance */
    __preSelectedItem : null,


    /*
    ---------------------------------------------------------------------------
      WIDGET API
    ---------------------------------------------------------------------------
    */

    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "spacer":
          control = new qx.ui.core.Spacer();
          this._add(control, {flex: 1});
          break;

        case "atom":
          control = new qx.ui.basic.Atom(" ");
          control.setCenter(false);
          control.setAnonymous(true);

          this._add(control, {flex:1});
          break;

        case "arrow":
          control = new qx.ui.basic.Image();
          control.setAnonymous(true);

          this._add(control);
          break;
      }

      return control || this.base(arguments, id);
    },

    // overridden
    /**
     * @lint ignoreReferenceField(_forwardStates)
     */
    _forwardStates : {
      focused : true
    },


    /*
    ---------------------------------------------------------------------------
      HELPER METHODS FOR SELECTION API
    ---------------------------------------------------------------------------
    */


    /**
     * Returns the list items for the selection.
     *
     * @return {qx.ui.form.ListItem[]} List itmes to select.
     */
    _getItems : function() {
      return this.getChildrenContainer().getChildren();
    },

    /**
     * Returns if the selection could be empty or not.
     *
     * @return {Boolean} <code>true</code> If selection could be empty,
     *    <code>false</code> otherwise.
     */
    _isAllowEmptySelection: function() {
      return this.getChildrenContainer().getSelectionMode() !== "one";
    },

    /**
     * Event handler for <code>changeSelection</code>.
     *
     * @param e {qx.event.type.Data} Data event.
     */
    __onChangeSelection : function(e)
    {
      var listItem = e.getData()[0];

      var list = this.getChildControl("list");
      if (list.getSelection()[0] != listItem) {
        if(listItem) {
          list.setSelection([listItem]);
        } else {
          list.resetSelection();
        }
      }

      this.__updateIcon();
      this.__updateLabel();
    },


    /**
     * Sets the icon inside the list to match the selected ListItem.
     */
    __updateIcon : function()
    {
      var listItem = this.getChildControl("list").getSelection()[0];
      var atom = this.getChildControl("atom");
      var icon = listItem ? listItem.getIcon() : "";
      icon == null ? atom.resetIcon() : atom.setIcon(icon);
    },

    /**
     * Sets the label inside the list to match the selected ListItem.
     */
    __updateLabel : function()
    {
      var listItem = this.getChildControl("list").getSelection()[0];
      var atom = this.getChildControl("atom");
      var label = listItem ? listItem.getLabel() : "";
      var format = this.getFormat();
      if (format != null) {
        label = format.call(this, listItem);
      }

      // check for translation
      if (label && label.translate) {
        label = label.translate();
      }
      label == null ? atom.resetLabel() : atom.setLabel(label);
    },


    /*
    ---------------------------------------------------------------------------
      EVENT LISTENERS
    ---------------------------------------------------------------------------
    */


    /**
     * Listener method for "mouseover" event
     * <ul>
     * <li>Adds state "hovered"</li>
     * <li>Removes "abandoned" and adds "pressed" state (if "abandoned" state is set)</li>
     * </ul>
     *
     * @param e {Event} Mouse event
     */
    _onMouseOver : function(e)
    {
      if (!this.isEnabled() || e.getTarget() !== this) {
        return;
      }

      if (this.hasState("abandoned"))
      {
        this.removeState("abandoned");
        this.addState("pressed");
      }

      this.addState("hovered");
    },

    /**
     * Listener method for "mouseout" event
     * <ul>
     * <li>Removes "hovered" state</li>
     * <li>Adds "abandoned" and removes "pressed" state (if "pressed" state is set)</li>
     * </ul>
     *
     * @param e {Event} Mouse event
     */
    _onMouseOut : function(e)
    {
      if (!this.isEnabled() || e.getTarget() !== this) {
        return;
      }

      this.removeState("hovered");

      if (this.hasState("pressed"))
      {
        this.removeState("pressed");
        this.addState("abandoned");
      }
    },

    /**
     * Toggles the popup's visibility.
     *
     * @param e {qx.event.type.Mouse} Mouse event
     */
    _onClick : function(e) {
      this.toggle();
    },

    /**
     * Event handler for mousewheel event
     *
     * @param e {qx.event.type.Mouse} Mouse event
     */
    _onMouseWheel : function(e)
    {
      if (this.getChildControl("popup").isVisible()) {
        return;
      }

      var direction = e.getWheelDelta("y") > 0 ? 1 : -1;
      var children = this.getSelectables();
      var selected = this.getSelection()[0];

      if (!selected) {
        if (!children[0]) {
          return;
        }
        selected = children[0];
      }

      var index = children.indexOf(selected) + direction;
      var max = children.length - 1;

      // Limit
      if (index < 0) {
        index = 0;
      } else if (index >= max) {
        index = max;
      }

      this.setSelection([children[index]]);

      // stop the propagation
      // prevent any other widget from receiving this event
      // e.g. place a selectbox widget inside a scroll container widget
      e.stopPropagation();
      e.preventDefault();
    },

    // overridden
    _onKeyPress : function(e)
    {
      var iden = e.getKeyIdentifier();
      if(iden == "Enter" || iden == "Space")
      {
        // Apply pre-selected item (translate quick selection to real selection)
        if (this.__preSelectedItem)
        {
          this.setSelection([this.__preSelectedItem]);
          this.__preSelectedItem = null;
        }

        this.toggle();
      }
      else
      {
        this.base(arguments, e);
      }
    },

    /**
     * Forwards key event to list widget.
     *
     * @param e {qx.event.type.KeyInput} Key event
     */
    _onKeyInput : function(e)
    {
      // clone the event and re-calibrate the event
      var clone = e.clone();
      clone.setTarget(this._list);
      clone.setBubbles(false);

      // forward it to the list
      this.getChildControl("list").dispatchEvent(clone);
    },

    // overridden
    _onListMouseDown : function(e)
    {
      // Apply pre-selected item (translate quick selection to real selection)
      if (this.__preSelectedItem)
      {
        this.setSelection([this.__preSelectedItem]);
        this.__preSelectedItem = null;
      }
    },

    // overridden
    _onListChangeSelection : function(e)
    {
      var current = e.getData();
      var old = e.getOldData();

      // Remove old listeners for icon and label changes.
      if (old && old.length > 0)
      {
        old[0].removeListener("changeIcon", this.__updateIcon, this);
        old[0].removeListener("changeLabel", this.__updateLabel, this);
      }


      if (current.length > 0)
      {
        // Ignore quick context (e.g. mouseover)
        // and configure the new value when closing the popup afterwards
        var popup = this.getChildControl("popup");
        var list = this.getChildControl("list");
        var context = list.getSelectionContext();

        if (popup.isVisible() && (context == "quick" || context == "key"))
        {
          this.__preSelectedItem = current[0];
        }
        else
        {
          this.setSelection([current[0]]);
          this.__preSelectedItem = null;
        }

        // Add listeners for icon and label changes
        current[0].addListener("changeIcon", this.__updateIcon, this);
        current[0].addListener("changeLabel", this.__updateLabel, this);
      }
      else
      {
        this.resetSelection();
      }
    },

    // overridden
    _onPopupChangeVisibility : function(e)
    {
      this.base(arguments, e);

      // Synchronize the current selection to the list selection
      // when the popup is closed. The list selection may be invalid
      // because of the quick selection handling which is not
      // directly applied to the selectbox
      var popup = this.getChildControl("popup");
      if (!popup.isVisible())
      {
        var list = this.getChildControl("list");

        // check if the list has any children before selecting
        if (list.hasChildren()) {
          list.setSelection(this.getSelection());
        }
      } else {
        // ensure that the list is never biger that the max list height and
        // the available space in the viewport
        var distance = popup.getLayoutLocation(this);
        var viewPortHeight = qx.bom.Viewport.getHeight();
        // distance to the bottom and top borders of the viewport
        var toTop = distance.top;
        var toBottom = viewPortHeight - distance.bottom;
        var availableHeigth = toTop > toBottom ? toTop : toBottom;

        var maxListHeight = this.getMaxListHeight();
        var list = this.getChildControl("list")
        if (maxListHeight == null || maxListHeight > availableHeigth) {
          list.setMaxHeight(availableHeigth);
        } else if (maxListHeight < availableHeigth) {
          list.setMaxHeight(maxListHeight);
        }
      }
    }

  },


  /*
  *****************************************************************************
     DESTRUCT
  *****************************************************************************
  */


  destruct : function() {
    this.__preSelectedItem = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Form interface for all form widgets which have date as their primary
 * data type like datechooser's.
 */
qx.Interface.define("qx.ui.form.IDateForm",
{
  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events :
  {
    /** Fired when the value was modified */
    "changeValue" : "qx.event.type.Data"
  },



  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
    ---------------------------------------------------------------------------
      VALUE PROPERTY
    ---------------------------------------------------------------------------
    */

    /**
     * Sets the element's value.
     *
     * @param value {Date|null} The new value of the element.
     */
    setValue : function(value) {
      return arguments.length == 1;
    },


    /**
     * Resets the element's value to its initial value.
     */
    resetValue : function() {},


    /**
     * The element's user set value.
     *
     * @return {Date|null} The value.
     */
    getValue : function() {}
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * A *date field* is like a combo box with the date as popup. As button to
 * open the calendar a calendar icon is shown at the right to the textfield.
 *
 * To be conform with all form widgets, the {@link qx.ui.form.IForm} interface
 * is implemented.
 *
 * The following example creates a date field and sets the current
 * date as selected.
 *
 * <pre class='javascript'>
 * var dateField = new qx.ui.form.DateField();
 * this.getRoot().add(dateField, {top: 20, left: 20});
 * dateField.setValue(new Date());
 * </pre>
 *
 * @childControl list {qx.ui.control.DateChooser} date chooser component
 * @childControl popup {qx.ui.popup.Popup} popup which shows the list control
 * @childControl textfield {qx.ui.form.TextField} text field for manual date entry
 * @childControl button {qx.ui.form.Button} button that opens the list control
 */
qx.Class.define("qx.ui.form.DateField",
{
  extend : qx.ui.core.Widget,
  include : [
    qx.ui.core.MRemoteChildrenHandling,
    qx.ui.form.MForm
  ],
  implement : [
    qx.ui.form.IForm,
    qx.ui.form.IDateForm
  ],


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    // set the layout
    var layout = new qx.ui.layout.HBox();
    this._setLayout(layout);
    layout.setAlignY("middle");

    // text field
    var textField = this._createChildControl("textfield");
    this._createChildControl("button");

    // register listeners
    this.addListener("click", this._onClick, this);
    this.addListener("blur", this._onBlur, this);

    // forward the focusin and focusout events to the textfield. The textfield
    // is not focusable so the events need to be forwarded manually.
    this.addListener("focusin", function(e) {
      textField.fireNonBubblingEvent("focusin", qx.event.type.Focus);
      textField.setTextSelection(0,0);
    }, this);

    this.addListener("focusout", function(e) {
      textField.fireNonBubblingEvent("focusout", qx.event.type.Focus);
    }, this);

    // initializes the DateField with the default format
    this._setDefaultDateFormat();

    // adds a locale change listener
    this._addLocaleChangeListener();
  },




  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events :
  {
    /** Whenever the value is changed this event is fired
     *
     *  Event data: The new text value of the field.
     */
    "changeValue" : "qx.event.type.Data"
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {

    /** The formatter, which converts the selected date to a string. **/
    dateFormat :
    {
      check : "qx.util.format.DateFormat",
      apply : "_applyDateFormat"
    },

    /**
     * String value which will be shown as a hint if the field is all of:
     * unset, unfocused and enabled. Set to null to not show a placeholder
     * text.
     */
    placeholder :
    {
      check : "String",
      nullable : true,
      apply : "_applyPlaceholder"
    },

    // overridden
    appearance :
    {
      refine : true,
      init : "datefield"
    },

    // overridden
    focusable :
    {
      refine : true,
      init : true
    },

    // overridden
    width :
    {
      refine : true,
      init : 120
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  statics :
  {
    __dateFormat : null,
    __formatter : null,

    /**
     * Get the shared default date formatter
     *
     * @return {qx.util.format.DateFormat} The shared date formatter
     */
    getDefaultDateFormatter : function()
    {
      var format = qx.locale.Date.getDateFormat("medium").toString();

      if (format == this.__dateFormat) {
        return this.__formatter;
      }

      if (this.__formatter) {
        this.__formatter.dispose();
      }

      this.__formatter = new qx.util.format.DateFormat(format, qx.locale.Manager.getInstance().getLocale());
      this.__dateFormat = format;

      return this.__formatter;
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __localeListenerId : null,


    /**
     * @lint ignoreReferenceField(_forwardStates)
     */
    _forwardStates : {
      focused : true,
      invalid : true
    },


    /*
    ---------------------------------------------------------------------------
      PROTECTED METHODS
    ---------------------------------------------------------------------------
    */
    /**
     * Sets the default date format which is returned by
     * {@link #getDefaultDateFormatter}. You can overrride this method to
     * define your own default format.
     */
    _setDefaultDateFormat : function() {
      this.setDateFormat(qx.ui.form.DateField.getDefaultDateFormatter());
    },


    /**
     * Checks for "qx.dynlocale" and adds a listener to the locale changes.
     * On every change, {@link #_setDefaultDateFormat} is called to reinitialize
     * the format. You can easily override that method to prevent that behavior.
     */
    _addLocaleChangeListener : function() {
      // listen for locale changes
      if (qx.core.Environment.get("qx.dynlocale"))
      {
        this.__localeListenerId =
          qx.locale.Manager.getInstance().addListener("changeLocale", function() {
            this._setDefaultDateFormat();
          }, this);
      }
    },


    /*
    ---------------------------------------------------------------------------
      PUBLIC METHODS
    ---------------------------------------------------------------------------
    */


    /**
    * This method sets the date, which will be formatted according to
    * #dateFormat to the date field. It will also select the date in the
    * calendar popup.
    *
    * @param value {Date} The date to set.
     */
    setValue : function(value)
    {
      // set the date to the textfield
      var textField = this.getChildControl("textfield");
      textField.setValue(this.getDateFormat().format(value));

      // set the date in the datechooser
      var dateChooser = this.getChildControl("list");
      dateChooser.setValue(value);
    },


    /**
     * Returns the current set date, parsed from the input-field
     * corresponding to the {@link #dateFormat}.
     * If the given text could not be parsed, <code>null</code> will be returned.
     *
     * @return {Date} The currently set date.
     */
    getValue : function()
    {
      // get the value of the textfield
      var textfieldValue = this.getChildControl("textfield").getValue();

      // return the parsed date
      try {
        return this.getDateFormat().parse(textfieldValue);
      } catch (ex) {
        return null;
      }
    },


    /**
     * Resets the DateField. The textfield will be empty and the datechooser
     * will also have no selection.
     */
    resetValue: function()
    {
      // set the date to the textfield
      var textField = this.getChildControl("textfield");
      textField.setValue("");

      // set the date in the datechooser
      var dateChooser = this.getChildControl("list");
      dateChooser.setValue(null);
    },


    /*
    ---------------------------------------------------------------------------
      LIST STUFF
    ---------------------------------------------------------------------------
    */

    /**
     * Shows the date chooser popup.
     */
    open : function()
    {
      var popup = this.getChildControl("popup");

      popup.placeToWidget(this, true);
      popup.show();
    },


    /**
     * Hides the date chooser popup.
     */
    close : function() {
      this.getChildControl("popup").hide();
    },


    /**
     * Toggles the date chooser popup visibility.
     */
    toggle : function()
    {
      var isListOpen = this.getChildControl("popup").isVisible();
      if (isListOpen) {
        this.close();
      } else {
        this.open();
      }
    },


    /*
    ---------------------------------------------------------------------------
      PROPERTY APPLY METHODS
    ---------------------------------------------------------------------------
    */

    // property apply routine
    _applyDateFormat : function(value, old)
    {
      // if old is undefined or null do nothing
      if (!old) {
        return;
      }

      // get the date with the old date format
      try
      {
        var textfield = this.getChildControl("textfield");
        var dateStr = textfield.getValue();
        var currentDate = old.parse(dateStr);
        textfield.setValue(value.format(currentDate));
      }
      catch (ex) {
        // do nothing if the former date could not be parsed
      }
    },


    // property apply routine
    _applyPlaceholder : function(value, old) {
      this.getChildControl("textfield").setPlaceholder(value);
    },


    /*
    ---------------------------------------------------------------------------
      WIDGET API
    ---------------------------------------------------------------------------
    */

    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "textfield":
          control = new qx.ui.form.TextField();
          control.setFocusable(false);
          control.addState("inner");
          control.addListener("changeValue", this._onTextFieldChangeValue, this);
          control.addListener("blur", this.close, this);
          this._add(control, {flex: 1});
          break;

        case "button":
          control = new qx.ui.form.Button();
          control.setFocusable(false);
          control.setKeepActive(true);
          control.addState("inner");
          control.addListener("execute", this.toggle, this);
          this._add(control);
          break;

        case "list":
          control = new qx.ui.control.DateChooser();
          control.setFocusable(false);
          control.setKeepFocus(true);
          control.addListener("execute", this._onChangeDate, this);
          break;

        case "popup":
          control = new qx.ui.popup.Popup(new qx.ui.layout.VBox);
          control.setAutoHide(false);
          control.add(this.getChildControl("list"));
          control.addListener("mouseup", this._onChangeDate, this);
          control.addListener("changeVisibility", this._onPopupChangeVisibility, this);
          break;
      }

      return control || this.base(arguments, id);
    },




   /*
   ---------------------------------------------------------------------------
     EVENT LISTENERS
   ---------------------------------------------------------------------------
   */

   /**
    * Handler method which handles the click on the calender popup.
    *
    * @param e {qx.event.type.Mouse} The mouse event of the click.
    */
    _onChangeDate : function(e)
    {
      var textField = this.getChildControl("textfield");

      var selectedDate = this.getChildControl("list").getValue();

      textField.setValue(this.getDateFormat().format(selectedDate));
      this.close();
    },


    /**
     * Toggles the popup's visibility.
     *
     * @param e {qx.event.type.Mouse} Mouse click event
     */
    _onClick : function(e) {
      this.close();
    },


    /**
     * Handler for the blur event of the current widget.
     *
     * @param e {qx.event.type.Focus} The blur event.
     */
    _onBlur : function(e)
    {
      this.close();
    },


    /**
     * Handler method which handles the key press. It forwards all key event
     * to the opened date chooser except the escape key event. Escape closes
     * the popup.
     * If the list is cloned, all key events will not be processed further.
     *
     * @param e {qx.event.type.KeySequence} Keypress event
     */
    _onKeyPress : function(e)
    {
      // get the key identifier
      var iden = e.getKeyIdentifier();
      if (iden == "Down" && e.isAltPressed())
      {
        this.toggle();
        e.stopPropagation();
        return;
      }

      // if the popup is closed, ignore all
      var popup = this.getChildControl("popup");
      if (popup.getVisibility() == "hidden") {
        return;
      }

      // hide the list always on escape
      if (iden == "Escape")
      {
        this.close();
        e.stopPropagation();
        return;
      }

      // Stop navigation keys when popup is open
      if (iden === "Left" || iden === "Right" || iden === "Down" || iden === "Up") {
        e.preventDefault();
      }

      // forward the rest of the events to the date chooser
      this.getChildControl("list").handleKeyPress(e);
    },


    /**
     * Redirects changeVisibility event from the list to this widget.
     *
     * @param e {qx.event.type.Data} Property change event
     */
    _onPopupChangeVisibility : function(e)
    {
      e.getData() == "visible" ? this.addState("popupOpen") : this.removeState("popupOpen");

      // Synchronize the chooser with the current value on every
      // opening of the popup. This is needed when the value has been
      // modified and not saved yet (e.g. no blur)
      var popup = this.getChildControl("popup");
      if (popup.isVisible())
      {
        var chooser = this.getChildControl("list");
        var date = this.getValue();
        chooser.setValue(date);
      }
    },


    /**
     * Reacts on value changes of the text field and syncs the
     * value to the combobox.
     *
     * @param e {qx.event.type.Data} Change event
     */
    _onTextFieldChangeValue : function(e)
    {
      // Apply to popup
      var date = this.getValue();
      if (date != null)
      {
        var list = this.getChildControl("list");
        list.setValue(date);
      }

      // Fire event
      this.fireDataEvent("changeValue", this.getValue());
    },


    /**
     * Checks if the textfield of the DateField is empty.
     *
     * @return {Boolean} True, if the textfield of the DateField is empty.
     */
    isEmpty: function()
    {
      var value = this.getChildControl("textfield").getValue();
      return value == null || value == "";
    }
  },


  destruct : function() {
    // listen for locale changes
    if (qx.core.Environment.get("qx.dynlocale"))
    {
      if (this.__localeListenerId) {
        qx.locale.Manager.getInstance().removeListenerById(this.__localeListenerId);
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * A *date chooser* is a small calendar including a navigation bar to switch the shown
 * month. It includes a column for the calendar week and shows one month. Selecting
 * a date is as easy as clicking on it.
 *
 * To be conform with all form widgets, the {@link qx.ui.form.IForm} interface
 * is implemented.
 *
 * The following example creates and adds a date chooser to the root element.
 * A listener alerts the user if a new date is selected.
 *
 * <pre class='javascript'>
 * var chooser = new qx.ui.control.DateChooser();
 * this.getRoot().add(chooser, { left : 20, top: 20});
 *
 * chooser.addListener("changeValue", function(e) {
 *   alert(e.getData());
 * });
 * </pre>
 *
 * Additionally to a selection event an execute event is available which is
 * fired by doubleclick or taping the space / enter key. With this event you
 * can for example save the selection and close the date chooser.
 *
 * @childControl navigation-bar {qx.ui.container.Composite} container for the navigation bar controls
 * @childControl last-year-button-tooltip {qx.ui.tooltip.ToolTip} tooltip for the last year button
 * @childControl last-year-button {qx.ui.form.Button} button to jump to the last year
 * @childControl last-month-button-tooltip {qx.ui.tooltip.ToolTip} tooltip for the last month button
 * @childControl last-month-button {qx.ui.form.Button} button to jump to the last month
 * @childControl next-month-button-tooltip {qx.ui.tooltip.ToolTip} tooltip for the next month button
 * @childControl next-month-button {qx.ui.form.Button} button to jump to the next month
 * @childControl next-year-button-tooltip {qx.ui.tooltip.ToolTip} tooltip for the next year button
 * @childControl next-year-button {qx.ui.form.Button} button to jump to the next year
 * @childControl month-year-label {qx.ui.basic.Label} shows the current month and year
 * @childControl week {qx.ui.basic.Label} week label (used multiple times)
 * @childControl weekday {qx.ui.basic.Label} weekday label (used multiple times)
 * @childControl day {qx.ui.basic.Label} day label (used multiple times)
 * @childControl date-pane {qx.ui.container.Composite} the pane used to position the week, weekday and day labels
 *
 */
qx.Class.define("qx.ui.control.DateChooser",
{
  extend : qx.ui.core.Widget,
  include : [
    qx.ui.core.MExecutable,
    qx.ui.form.MForm
  ],
  implement : [
    qx.ui.form.IExecutable,
    qx.ui.form.IForm,
    qx.ui.form.IDateForm
  ],


  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param date {Date ? null} The initial date to show. If <code>null</code>
   * the current day (today) is shown.
   */
  construct : function(date)
  {
    this.base(arguments);

    // set the layout
    var layout = new qx.ui.layout.VBox();
    this._setLayout(layout);

    // create the child controls
    this._createChildControl("navigation-bar");
    this._createChildControl("date-pane");

    // Support for key events
    this.addListener("keypress", this._onKeyPress);

    // initialize format - moved from statics{} to constructor due to [BUG #7149]
    var DateChooser = qx.ui.control.DateChooser;
    if (!DateChooser.MONTH_YEAR_FORMAT) {
        DateChooser.MONTH_YEAR_FORMAT = qx.locale.Date.getDateTimeFormat("yyyyMMMM", "MMMM yyyy");
    }

    // Show the right date
    var shownDate = (date != null) ? date : new Date();
    this.showMonth(shownDate.getMonth(), shownDate.getFullYear());

    // listen for locale changes
    if (qx.core.Environment.get("qx.dynlocale")) {
      qx.locale.Manager.getInstance().addListener("changeLocale", this._updateDatePane, this);
    }

    // register mouse up and down handler
    this.addListener("mousedown", this._onMouseUpDown, this);
    this.addListener("mouseup", this._onMouseUpDown, this);
  },



  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

  statics :
  {
    /**
     * @type {string} The format for the date year label at the top center.
     */
    MONTH_YEAR_FORMAT : null,

    /**
     * @type {string} The format for the weekday labels (the headers of the date table).
     */
    WEEKDAY_FORMAT : "EE",

    /**
     * @type {string} The format for the week numbers (the labels of the left column).
     */
    WEEK_FORMAT : "ww"
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init   : "datechooser"
    },

    // overrridden
    width :
    {
      refine : true,
      init : 200
    },

    // overridden
    height :
    {
      refine : true,
      init : 150
    },

    /** The currently shown month. 0 = january, 1 = february, and so on. */
    shownMonth :
    {
      check : "Integer",
      init : null,
      nullable : true,
      event : "changeShownMonth"
    },

    /** The currently shown year. */
    shownYear :
    {
      check : "Integer",
      init : null,
      nullable : true,
      event : "changeShownYear"
    },

    /** The date value of the widget. */
    value :
    {
      check : "Date",
      init : null,
      nullable : true,
      event : "changeValue",
      apply : "_applyValue"
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __weekdayLabelArr : null,
    __dayLabelArr : null,
    __weekLabelArr : null,


    // overridden
    /**
     * @lint ignoreReferenceField(_forwardStates)
     */
    _forwardStates :
    {
      invalid : true
    },


    /*
    ---------------------------------------------------------------------------
      WIDGET INTERNALS
    ---------------------------------------------------------------------------
    */

    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        // NAVIGATION BAR STUFF
        case "navigation-bar":
          control = new qx.ui.container.Composite(new qx.ui.layout.HBox());

          // Add the navigation bar elements
          control.add(this.getChildControl("last-year-button"));
          control.add(this.getChildControl("last-month-button"));
          control.add(this.getChildControl("month-year-label"), {flex: 1});
          control.add(this.getChildControl("next-month-button"));
          control.add(this.getChildControl("next-year-button"));

          this._add(control);
          break;

        case "last-year-button-tooltip":
          control = new qx.ui.tooltip.ToolTip(this.tr("Last year"));
          break;

        case "last-year-button":
          control = new qx.ui.toolbar.Button();
          control.addState("lastYear");
          control.setFocusable(false);
          control.setToolTip(this.getChildControl("last-year-button-tooltip"));
          control.addListener("click", this._onNavButtonClicked, this);
          break;

        case "last-month-button-tooltip":
          control = new qx.ui.tooltip.ToolTip(this.tr("Last month"));
          break;

        case "last-month-button":
          control = new qx.ui.toolbar.Button();
          control.addState("lastMonth");
          control.setFocusable(false);
          control.setToolTip(this.getChildControl("last-month-button-tooltip"));
          control.addListener("click", this._onNavButtonClicked, this);
          break;

        case "next-month-button-tooltip":
          control = new qx.ui.tooltip.ToolTip(this.tr("Next month"));
          break;

        case "next-month-button":
          control = new qx.ui.toolbar.Button();
          control.addState("nextMonth");
          control.setFocusable(false);
          control.setToolTip(this.getChildControl("next-month-button-tooltip"));
          control.addListener("click", this._onNavButtonClicked, this);
          break;

        case "next-year-button-tooltip":
          control = new qx.ui.tooltip.ToolTip(this.tr("Next year"));
          break;

        case "next-year-button":
          control = new qx.ui.toolbar.Button();
          control.addState("nextYear");
          control.setFocusable(false);
          control.setToolTip(this.getChildControl("next-year-button-tooltip"));
          control.addListener("click", this._onNavButtonClicked, this);
          break;

        case "month-year-label":
          control = new qx.ui.basic.Label();
          control.setAllowGrowX(true);
          control.setAnonymous(true);
          break;

        case "week":
          control = new qx.ui.basic.Label();
          control.setAllowGrowX(true);
          control.setAllowGrowY(true);
          control.setSelectable(false);
          control.setAnonymous(true);
          control.setCursor("default");
          break;

        case "weekday":
          control = new qx.ui.basic.Label();
          control.setAllowGrowX(true);
          control.setAllowGrowY(true);
          control.setSelectable(false);
          control.setAnonymous(true);
          control.setCursor("default");
          break;

        case "day":
          control = new qx.ui.basic.Label();
          control.setAllowGrowX(true);
          control.setAllowGrowY(true);
          control.setCursor("default");
          control.addListener("mousedown", this._onDayClicked, this);
          control.addListener("dblclick", this._onDayDblClicked, this);
          break;

        case "date-pane":
          var controlLayout = new qx.ui.layout.Grid()
          control = new qx.ui.container.Composite(controlLayout);

          for (var i = 0; i < 8; i++) {
            controlLayout.setColumnFlex(i, 1);
          }

          for (var i = 0; i < 7; i++) {
            controlLayout.setRowFlex(i, 1);
          }

          // Create the weekdays
          // Add an empty label as spacer for the week numbers
          var label = this.getChildControl("week#0");
          label.addState("header");
          control.add(label, {column: 0, row: 0});

          this.__weekdayLabelArr = [];
          for (var i=0; i<7; i++)
          {
            label = this.getChildControl("weekday#" + i);
            control.add(label, {column: i + 1, row: 0});
            this.__weekdayLabelArr.push(label);
          }

          // Add the days
          this.__dayLabelArr = [];
          this.__weekLabelArr = [];

          for (var y = 0; y < 6; y++)
          {
            // Add the week label
            var label = this.getChildControl("week#" + (y+1));
            control.add(label, {column: 0, row: y + 1});
            this.__weekLabelArr.push(label);

            // Add the day labels
            for (var x = 0; x < 7; x++)
            {
              var label = this.getChildControl("day#" + ((y*7)+x));
              control.add(label, {column:x + 1, row:y + 1});
              this.__dayLabelArr.push(label);
            }
          }

          this._add(control);
          break;
      }

      return control || this.base(arguments, id);
    },


    // apply methods
    _applyValue : function(value, old)
    {
      if ((value != null) && (this.getShownMonth() != value.getMonth() || this.getShownYear() != value.getFullYear()))
      {
        // The new date is in another month -> Show that month
        this.showMonth(value.getMonth(), value.getFullYear());
      }
      else
      {
        // The new date is in the current month -> Just change the states
        var newDay = (value == null) ? -1 : value.getDate();

        for (var i=0; i<6*7; i++)
        {
          var dayLabel = this.__dayLabelArr[i];

          if (dayLabel.hasState("otherMonth"))
          {
            if (dayLabel.hasState("selected")) {
              dayLabel.removeState("selected");
            }
          }
          else
          {
            var day = parseInt(dayLabel.getValue(), 10);

            if (day == newDay) {
              dayLabel.addState("selected");
            } else if (dayLabel.hasState("selected")) {
              dayLabel.removeState("selected");
            }
          }
        }
      }
    },



    /*
    ---------------------------------------------------------------------------
      EVENT HANDLER
    ---------------------------------------------------------------------------
    */

    /**
     * Handler which stops the propagation of the click event if
     * the navigation bar or calendar headers will be clicked.
     *
     * @param e {qx.event.type.Mouse} The mouse up / down event
     */
    _onMouseUpDown : function(e) {
      var target = e.getTarget();

      if (target == this.getChildControl("navigation-bar") ||
          target == this.getChildControl("date-pane")) {
        e.stopPropagation();
        return;
      }
    },


    /**
     * Event handler. Called when a navigation button has been clicked.
     *
     * @param evt {qx.event.type.Data} The data event.
     */
    _onNavButtonClicked : function(evt)
    {
      var year = this.getShownYear();
      var month = this.getShownMonth();

      switch(evt.getCurrentTarget())
      {
        case this.getChildControl("last-year-button"):
          year--;
          break;

        case this.getChildControl("last-month-button"):
          month--;

          if (month < 0)
          {
            month = 11;
            year--;
          }

          break;

        case this.getChildControl("next-month-button"):
          month++;

          if (month >= 12)
          {
            month = 0;
            year++;
          }

          break;

        case this.getChildControl("next-year-button"):
          year++;
          break;
      }

      this.showMonth(month, year);
    },


    /**
     * Event handler. Called when a day has been clicked.
     *
     * @param evt {qx.event.type.Data} The event.
     */
    _onDayClicked : function(evt)
    {
      var time = evt.getCurrentTarget().dateTime;
      this.setValue(new Date(time));
    },


    /**
     * Event handler. Called when a day has been double-clicked.
     */
    _onDayDblClicked : function() {
      this.execute();
    },


    /**
     * Event handler. Called when a key was pressed.
     *
     * @param evt {qx.event.type.Data} The event.
     */
    _onKeyPress : function(evt)
    {
      var dayIncrement = null;
      var monthIncrement = null;
      var yearIncrement = null;

      if (evt.getModifiers() == 0)
      {
        switch(evt.getKeyIdentifier())
        {
          case "Left":
            dayIncrement = -1;
            break;

          case "Right":
            dayIncrement = 1;
            break;

          case "Up":
            dayIncrement = -7;
            break;

          case "Down":
            dayIncrement = 7;
            break;

          case "PageUp":
            monthIncrement = -1;
            break;

          case "PageDown":
            monthIncrement = 1;
            break;

          case "Escape":
            if (this.getValue() != null)
            {
              this.setValue(null);
              return;
            }

            break;

          case "Enter":
          case "Space":
            if (this.getValue() != null) {
              this.execute();
            }

            return;
        }
      }
      else if (evt.isShiftPressed())
      {
        switch(evt.getKeyIdentifier())
        {
          case "PageUp":
            yearIncrement = -1;
            break;

          case "PageDown":
            yearIncrement = 1;
            break;
        }
      }

      if (dayIncrement != null || monthIncrement != null || yearIncrement != null)
      {
        var date = this.getValue();

        if (date != null) {
          date = new Date(date.getTime());
        }

        if (date == null) {
          date = new Date();
        }
        else
        {
          if (dayIncrement != null){date.setDate(date.getDate() + dayIncrement);}
          if (monthIncrement != null){date.setMonth(date.getMonth() + monthIncrement);}
          if (yearIncrement != null){date.setFullYear(date.getFullYear() + yearIncrement);}
        }

        this.setValue(date);
      }
    },


    /**
     * Shows a certain month.
     *
     * @param month {Integer ? null} the month to show (0 = january). If not set
     *      the month will remain the same.
     * @param year {Integer ? null} the year to show. If not set the year will
     *      remain the same.
     */
    showMonth : function(month, year)
    {
      if ((month != null && month != this.getShownMonth()) || (year != null && year != this.getShownYear()))
      {
        if (month != null) {
          this.setShownMonth(month);
        }

        if (year != null) {
          this.setShownYear(year);
        }

        this._updateDatePane();
      }
    },


    /**
     * Event handler. Used to handle the key events.
     *
     * @param e {qx.event.type.Data} The event.
     */
    handleKeyPress : function(e) {
      this._onKeyPress(e);
    },


    /**
     * Updates the date pane.
     */
    _updateDatePane : function()
    {
      var DateChooser = qx.ui.control.DateChooser;

      var today = new Date();
      var todayYear = today.getFullYear();
      var todayMonth = today.getMonth();
      var todayDayOfMonth = today.getDate();

      var selDate = this.getValue();
      var selYear = (selDate == null) ? -1 : selDate.getFullYear();
      var selMonth = (selDate == null) ? -1 : selDate.getMonth();
      var selDayOfMonth = (selDate == null) ? -1 : selDate.getDate();

      var shownMonth = this.getShownMonth();
      var shownYear = this.getShownYear();

      var startOfWeek = qx.locale.Date.getWeekStart();

      // Create a help date that points to the first of the current month
      var helpDate = new Date(this.getShownYear(), this.getShownMonth(), 1);

      var monthYearFormat = new qx.util.format.DateFormat(DateChooser.MONTH_YEAR_FORMAT);
      this.getChildControl("month-year-label").setValue(monthYearFormat.format(helpDate));

      // Show the day names
      var firstDayOfWeek = helpDate.getDay();
      var firstSundayInMonth = 1 + ((7 - firstDayOfWeek) % 7);
      var weekDayFormat = new qx.util.format.DateFormat(DateChooser.WEEKDAY_FORMAT);

      for (var i=0; i<7; i++)
      {
        var day = (i + startOfWeek) % 7;

        var dayLabel = this.__weekdayLabelArr[i];

        helpDate.setDate(firstSundayInMonth + day);
        dayLabel.setValue(weekDayFormat.format(helpDate));

        if (qx.locale.Date.isWeekend(day)) {
          dayLabel.addState("weekend");
        } else {
          dayLabel.removeState("weekend");
        }
      }

      // Show the days
      helpDate = new Date(shownYear, shownMonth, 1, 12, 0, 0);
      var nrDaysOfLastMonth = (7 + firstDayOfWeek - startOfWeek) % 7;
      helpDate.setDate(helpDate.getDate() - nrDaysOfLastMonth);

      var weekFormat = new qx.util.format.DateFormat(DateChooser.WEEK_FORMAT);

      for (var week=0; week<6; week++)
      {
        this.__weekLabelArr[week].setValue(weekFormat.format(helpDate));

        for (var i=0; i<7; i++)
        {
          var dayLabel = this.__dayLabelArr[week * 7 + i];

          var year = helpDate.getFullYear();
          var month = helpDate.getMonth();
          var dayOfMonth = helpDate.getDate();

          var isSelectedDate = (selYear == year && selMonth == month && selDayOfMonth == dayOfMonth);

          if (isSelectedDate) {
            dayLabel.addState("selected");
          } else {
            dayLabel.removeState("selected");
          }

          if (month != shownMonth) {
            dayLabel.addState("otherMonth");
          } else {
            dayLabel.removeState("otherMonth");
          }

          var isToday = (year == todayYear && month == todayMonth && dayOfMonth == todayDayOfMonth);

          if (isToday) {
            dayLabel.addState("today");
          } else {
            dayLabel.removeState("today");
          }

          dayLabel.setValue("" + dayOfMonth);
          dayLabel.dateTime = helpDate.getTime();

          // Go to the next day
          helpDate.setDate(helpDate.getDate() + 1);
        }
      }

      monthYearFormat.dispose();
      weekDayFormat.dispose();
      weekFormat.dispose();
    }
  },




  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    if (qx.core.Environment.get("qx.dynlocale")) {
      qx.locale.Manager.getInstance().removeListener("changeLocale", this._updateDatePane, this);
    }

    this.__weekdayLabelArr = this.__dayLabelArr = this.__weekLabelArr = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)

************************************************************************ */

/**
 * The normal toolbar button. Like a normal {@link qx.ui.form.Button}
 * but with a style matching the toolbar and without keyboard support.
 */
qx.Class.define("qx.ui.toolbar.Button",
{
  extend : qx.ui.form.Button,



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function(label, icon, command)
  {
    this.base(arguments, label, icon, command);

    // Toolbar buttons should not support the keyboard events
    this.removeListener("keydown", this._onKeyDown);
    this.removeListener("keyup", this._onKeyUp);
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    appearance :
    {
      refine : true,
      init : "toolbar-button"
    },

    show :
    {
      refine : true,
      init : "inherit"
    },

    focusable :
    {
      refine : true,
      init : false
    }
  },

  members : {
    // overridden
    _applyVisibility : function(value, old) {
      this.base(arguments, value, old);
      // trigger a appearance recalculation of the parent
      var parent = this.getLayoutParent();
      if (parent && parent instanceof qx.ui.toolbar.PartContainer) {
        qx.ui.core.queue.Appearance.add(parent);
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * The form object is responsible for managing form items. For that, it takes
 * advantage of two existing qooxdoo classes.
 * The {@link qx.ui.form.Resetter} is used for resetting and the
 * {@link qx.ui.form.validation.Manager} is used for all validation purposes.
 *
 * The view code can be found in the used renderer ({@link qx.ui.form.renderer}).
 */
qx.Class.define("qx.ui.form.Form",
{
  extend : qx.core.Object,


  construct : function()
  {
    this.base(arguments);

    this.__groups = [];
    this._buttons = [];
    this._buttonOptions = [];
    this._validationManager = new qx.ui.form.validation.Manager();
    this._resetter = this._createResetter();
  },


  members :
  {
    __groups : null,
    _validationManager : null,
    _groupCounter : 0,
    _buttons : null,
    _buttonOptions : null,
    _resetter : null,

    /*
    ---------------------------------------------------------------------------
       ADD
    ---------------------------------------------------------------------------
    */

    /**
     * Adds a form item to the form including its internal
     * {@link qx.ui.form.validation.Manager} and {@link qx.ui.form.Resetter}.
     *
     * *Hint:* The order of all add calls represent the order in the layout.
     *
     * @param item {qx.ui.form.IForm} A supported form item.
     * @param label {String} The string, which should be used as label.
     * @param validator {Function | qx.ui.form.validation.AsyncValidator ? null}
     *   The validator which is used by the validation
     *   {@link qx.ui.form.validation.Manager}.
     * @param name {String?null} The name which is used by the data binding
     *   controller {@link qx.data.controller.Form}.
     * @param validatorContext {var?null} The context of the validator.
     * @param options {Map?null} An additional map containin custom data which
     *   will be available in your form renderer specific to the added item.
     */
    add : function(item, label, validator, name, validatorContext, options) {
      if (this.__isFirstAdd()) {
        this.__groups.push({
          title: null, items: [], labels: [], names: [],
          options: [], headerOptions: {}
        });
      }
      // save the given arguments
      this.__groups[this._groupCounter].items.push(item);
      this.__groups[this._groupCounter].labels.push(label);
      this.__groups[this._groupCounter].options.push(options);
      // if no name is given, use the label without not working character
      if (name == null) {
        name = label.replace(
          /\s+|&|-|\+|\*|\/|\||!|\.|,|:|\?|;|~|%|\{|\}|\(|\)|\[|\]|<|>|=|\^|@|\\/g, ""
        );
      }
      this.__groups[this._groupCounter].names.push(name);

      // add the item to the validation manager
      this._validationManager.add(item, validator, validatorContext);
      // add the item to the reset manager
      this._resetter.add(item);
    },


    /**
     * Adds a group header to the form.
     *
     * *Hint:* The order of all add calls represent the order in the layout.
     *
     * @param title {String} The title of the group header.
     * @param options {Map?null} A special set of custom data which will be
     *   given to the renderer.
     */
    addGroupHeader : function(title, options) {
      if (!this.__isFirstAdd()) {
        this._groupCounter++;
      }
      this.__groups.push({
        title: title, items: [], labels: [], names: [],
        options: [], headerOptions: options
      });
    },


    /**
     * Adds a button to the form.
     *
     * *Hint:* The order of all add calls represent the order in the layout.
     *
     * @param button {qx.ui.form.Button} The button to add.
     * @param options {Map?null} An additional map containin custom data which
     *   will be available in your form renderer specific to the added button.
     */
    addButton : function(button, options) {
      this._buttons.push(button);
      this._buttonOptions.push(options || null);
    },


    /**
     * Returns whether something has already been added.
     *
     * @return {Boolean} true, if nothing has been added jet.
     */
    __isFirstAdd : function() {
      return this.__groups.length === 0;
    },


    /*
    ---------------------------------------------------------------------------
       RESET SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * Resets the form. This means reseting all form items and the validation.
     */
    reset : function() {
      this._resetter.reset();
      this._validationManager.reset();
    },


    /**
     * Redefines the values used for resetting. It calls
     * {@link qx.ui.form.Resetter#redefine} to get that.
     */
    redefineResetter : function() {
      this._resetter.redefine();
    },


    /**
     * Redefines the value used for resetting of the given item. It calls
     * {@link qx.ui.form.Resetter#redefineItem} to get that.
     *
     * @param item {qx.ui.core.Widget} The item to redefine.
     */
    redefineResetterItem : function(item) {
      this._resetter.redefineItem(item);
    },



    /*
    ---------------------------------------------------------------------------
       VALIDATION
    ---------------------------------------------------------------------------
    */

    /**
     * Validates the form using the
     * {@link qx.ui.form.validation.Manager#validate} method.
     *
     * @return {Boolean | null} The validation result.
     */
    validate : function() {
      return this._validationManager.validate();
    },


    /**
     * Returns the internally used validation manager. If you want to do some
     * enhanced validation tasks, you need to use the validation manager.
     *
     * @return {qx.ui.form.validation.Manager} The used manager.
     */
    getValidationManager : function() {
      return this._validationManager;
    },


    /*
    ---------------------------------------------------------------------------
       RENDERER SUPPORT
    ---------------------------------------------------------------------------
    */

    /**
     * Accessor method for the renderer which returns all added items in a
     * array containing a map of all items:
     * {title: title, items: [], labels: [], names: []}
     *
     * @return {Array} An array containing all necessary data for the renderer.
     * @internal
     */
    getGroups : function() {
      return this.__groups;
    },


    /**
     * Accessor method for the renderer which returns all added buttons in an
     * array.
     * @return {Array} An array containing all added buttons.
     * @internal
     */
    getButtons : function() {
      return this._buttons;
    },


    /**
     * Accessor method for the renderer which returns all added options for
     * the buttons in an array.
     * @return {Array} An array containing all added options for the buttons.
     * @internal
     */
    getButtonOptions : function() {
      return this._buttonOptions;
    },



    /*
    ---------------------------------------------------------------------------
       INTERNAL
    ---------------------------------------------------------------------------
    */

    /**
     * Returns all added items as a map.
     *
     * @return {Map} A map containing for every item an entry with its name.
     *
     * @internal
     */
    getItems : function() {
      var items = {};
      // go threw all groups
      for (var i = 0; i < this.__groups.length; i++) {
        var group = this.__groups[i];
        // get all items
        for (var j = 0; j < group.names.length; j++) {
          var name = group.names[j];
          items[name] = group.items[j];
        }
      }
      return items;
    },


    /**
     * Creates and returns the used resetter class.
     *
     * @return {qx.ui.form.Resetter} the resetter class.
     *
     * @internal
     */
    _createResetter : function() {
      return new qx.ui.form.Resetter();
    }
  },


  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */
  destruct : function()
  {
    // holding references to widgets --> must set to null
    this.__groups = this._buttons = this._buttonOptions = null;
    this._validationManager.dispose();
    this._resetter.dispose();
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */
/**
 * This validation manager is responsible for validation of forms.
 */
qx.Class.define("qx.ui.form.validation.Manager",
{
  extend : qx.core.Object,

  construct : function()
  {
    this.base(arguments);

    // storage for all form items
    this.__formItems = [];
    // storage for all results of async validation calls
    this.__asyncResults = {};
    // set the default required field message
    this.setRequiredFieldMessage(qx.locale.Manager.tr("This field is required"));
  },


  events :
  {
    /**
     * Change event for the valid state.
     */
    "changeValid" : "qx.event.type.Data",

    /**
     * Signals that the validation is done. This is not needed on synchronous
     * validation (validation is done right after the call) but very important
     * in the case an asynchronous validator will be used.
     */
    "complete" : "qx.event.type.Event"
  },


  properties :
  {
    /**
     * The validator of the form itself. You can set a function (for
     * synchronous validation) or a {@link qx.ui.form.validation.AsyncValidator}.
     * In both cases, the function can have all added form items as first
     * argument and the manager as a second argument. The manager should be used
     * to set the {@link #invalidMessage}.
     *
     * Keep in mind that the validator is optional if you don't need the
     * validation in the context of the whole form.
     * @type {Function | AsyncValidator}
     */
    validator :
    {
      check : "value instanceof Function || qx.Class.isSubClassOf(value.constructor, qx.ui.form.validation.AsyncValidator)",
      init : null,
      nullable : true
    },

    /**
     * The invalid message should store the message why the form validation
     * failed. It will be added to the array returned by
     * {@link #getInvalidMessages}.
     */
    invalidMessage :
    {
      check : "String",
      init: ""
    },


    /**
     * This message will be shown if a required field is empty and no individual
     * {@link qx.ui.form.MForm#requiredInvalidMessage} is given.
     */
    requiredFieldMessage :
    {
      check : "String",
      init : ""
    },


    /**
     * The context for the form validation.
     */
    context :
    {
      nullable : true
    }
  },


  members :
  {
    __formItems : null,
    __valid : null,
    __asyncResults : null,
    __syncValid : null,


    /**
     * Add a form item to the validation manager.
     *
     * The form item has to implement at least two interfaces:
     * <ol>
     *   <li>The {@link qx.ui.form.IForm} Interface</li>
     *   <li>One of the following interfaces:
     *     <ul>
     *       <li>{@link qx.ui.form.IBooleanForm}</li>
     *       <li>{@link qx.ui.form.IColorForm}</li>
     *       <li>{@link qx.ui.form.IDateForm}</li>
     *       <li>{@link qx.ui.form.INumberForm}</li>
     *       <li>{@link qx.ui.form.IStringForm}</li>
     *     </ul>
     *   </li>
     * </ol>
     * The validator can be a synchronous or asynchronous validator. In
     * both cases the validator can either returns a boolean or fire an
     * {@link qx.core.ValidationError}. For synchronous validation, a plain
     * JavaScript function should be used. For all asynchronous validations,
     * a {@link qx.ui.form.validation.AsyncValidator} is needed to wrap the
     * plain function.
     *
     * @param formItem {qx.ui.core.Widget} The form item to add.
     * @param validator {Function | qx.ui.form.validation.AsyncValidator}
     *   The validator.
     * @param context {var?null} The context of the validator.
     */
    add: function(formItem, validator, context) {
      // check for the form API
      if (!this.__supportsInvalid(formItem)) {
        throw new Error("Added widget not supported.");
      }
      // check for the data type
      if (this.__supportsSingleSelection(formItem) && !formItem.getValue) {
        // check for a validator
        if (validator != null) {
          throw new Error("Widgets supporting selection can only be validated " +
          "in the form validator");
        }
      }
      var dataEntry =
      {
        item : formItem,
        validator : validator,
        valid : null,
        context : context
      };
      this.__formItems.push(dataEntry);
    },


    /**
     * Remove a form item from the validation manager.
     *
     * @param formItem {qx.ui.core.Widget} The form item to remove.
     * @return {qx.ui.core.Widget?null} The removed form item or
     *  <code>null</code> if the item could not be found.
     */
    remove : function(formItem)
    {
      var items = this.__formItems;

      for (var i = 0, len = items.length; i < len; i++)
      {
        if (formItem === items[i].item)
        {
          items.splice(i, 1);
          return formItem;
        }
      }

      return null;
    },


    /**
     * Returns registered form items from the validation manager.
     *
     * @return {Array} The form items which will be validated.
     */
    getItems : function()
    {
      var items = [];
      for (var i=0; i < this.__formItems.length; i++) {
        items.push(this.__formItems[i].item);
      };
      return items;
    },


    /**
     * Invokes the validation. If only synchronous validators are set, the
     * result of the whole validation is available at the end of the method
     * and can be returned. If an asynchronous validator is set, the result
     * is still unknown at the end of this method so nothing will be returned.
     * In both cases, a {@link #complete} event will be fired if the validation
     * has ended. The result of the validation can then be accessed with the
     * {@link #getValid} method.
     *
     * @return {Boolean|undefined} The validation result, if available.
     */
    validate : function() {
      var valid = true;
      this.__syncValid = true; // collaboration of all synchronous validations
      var items = [];

      // check all validators for the added form items
      for (var i = 0; i < this.__formItems.length; i++) {
        var formItem = this.__formItems[i].item;
        var validator = this.__formItems[i].validator;

        // store the items in case of form validation
        items.push(formItem);

        // ignore all form items without a validator
        if (validator == null) {
          // check for the required property
          var validatorResult = this.__validateRequired(formItem);
          valid = valid && validatorResult;
          this.__syncValid = validatorResult && this.__syncValid;
          continue;
        }

        var validatorResult = this.__validateItem(
          this.__formItems[i], formItem.getValue()
        );
        // keep that order to ensure that null is returned on async cases
        valid = validatorResult && valid;
        if (validatorResult != null) {
          this.__syncValid = validatorResult && this.__syncValid;
        }
      }

      // check the form validator (be sure to invoke it even if the form
      // items are already false, so keep the order!)
      var formValid = this.__validateForm(items);
      if (qx.lang.Type.isBoolean(formValid)) {
        this.__syncValid = formValid && this.__syncValid;
      }
      valid = formValid && valid;

      this.__setValid(valid);

      if (qx.lang.Object.isEmpty(this.__asyncResults)) {
        this.fireEvent("complete");
      }
      return valid;
    },


    /**
     * Checks if the form item is required. If so, the value is checked
     * and the result will be returned. If the form item is not required, true
     * will be returned.
     *
     * @param formItem {qx.ui.core.Widget} The form item to check.
     * @return {var} Validation result
     */
    __validateRequired : function(formItem) {
      if (formItem.getRequired()) {
        // if its a widget supporting the selection
        if (this.__supportsSingleSelection(formItem)) {
          var validatorResult = !!formItem.getSelection()[0];
        // otherwise, a value should be supplied
        } else {
          var value = formItem.getValue();
          var validatorResult = !!value || value === 0;
        }
        formItem.setValid(validatorResult);
        var individualMessage = formItem.getRequiredInvalidMessage();
        var message = individualMessage ? individualMessage : this.getRequiredFieldMessage();
        formItem.setInvalidMessage(message);
        return validatorResult;
      }
      return true;
    },


    /**
     * Validates a form item. This method handles the differences of
     * synchronous and asynchronous validation and returns the result of the
     * validation if possible (synchronous cases). If the validation is
     * asynchronous, null will be returned.
     *
     * @param dataEntry {Object} The map stored in {@link #add}
     * @param value {var} The currently set value
     * @return {Boolean|null} Validation result or <code>null</code> for async
     * validation
     */
    __validateItem : function(dataEntry, value) {
      var formItem = dataEntry.item;
      var context = dataEntry.context;
      var validator = dataEntry.validator;

      // check for asynchronous validation
      if (this.__isAsyncValidator(validator)) {
        // used to check if all async validations are done
        this.__asyncResults[formItem.toHashCode()] = null;
        validator.validate(formItem, formItem.getValue(), this, context);
        return null;
      }

      var validatorResult = null;

      try {
        var validatorResult = validator.call(context || this, value, formItem);
        if (validatorResult === undefined) {
          validatorResult = true;
        }

      } catch (e) {
        if (e instanceof qx.core.ValidationError) {
          validatorResult = false;
          if (e.message && e.message != qx.type.BaseError.DEFAULTMESSAGE) {
            var invalidMessage = e.message;
          } else {
            var invalidMessage = e.getComment();
          }
          formItem.setInvalidMessage(invalidMessage);
        } else {
          throw e;
        }
      }

      formItem.setValid(validatorResult);
      dataEntry.valid = validatorResult;

      return validatorResult;
    },


    /**
     * Validates the form. It checks for asynchronous validation and handles
     * the differences to synchronous validation. If no form validator is given,
     * true will be returned. If a synchronous validator is given, the
     * validation result will be returned. In asynchronous cases, null will be
     * returned cause the result is not available.
     *
     * @param items {qx.ui.core.Widget[]} An array of all form items.
     * @return {Boolean|null} description
     */
    __validateForm: function(items) {
      var formValidator = this.getValidator();
      var context = this.getContext() || this;

      if (formValidator == null) {
        return true;
      }

      // reset the invalidMessage
      this.setInvalidMessage("");

      if (this.__isAsyncValidator(formValidator)) {
        this.__asyncResults[this.toHashCode()] = null;
        formValidator.validateForm(items, this, context);
        return null;
      }

      try {
        var formValid = formValidator.call(context, items, this);
        if (formValid === undefined) {
          formValid = true;
        }
      } catch (e) {
        if (e instanceof qx.core.ValidationError) {
          formValid = false;

          if (e.message && e.message != qx.type.BaseError.DEFAULTMESSAGE) {
            var invalidMessage = e.message;
          } else {
            var invalidMessage = e.getComment();
          }
          this.setInvalidMessage(invalidMessage);
        } else {
          throw e;
        }
      }
      return formValid;
    },


    /**
     * Helper function which checks, if the given validator is synchronous
     * or asynchronous.
     *
     * @param validator {Function|qx.ui.form.validation.AsyncValidator}
     *   The validator to check.
     * @return {Boolean} True, if the given validator is asynchronous.
     */
    __isAsyncValidator : function(validator) {
      var async = false;
      if (!qx.lang.Type.isFunction(validator)) {
        async = qx.Class.isSubClassOf(
          validator.constructor, qx.ui.form.validation.AsyncValidator
        );
      }
      return async;
    },


    /**
     * Returns true, if the given item implements the {@link qx.ui.form.IForm}
     * interface.
     *
     * @param formItem {qx.core.Object} The item to check.
     * @return {Boolean} true, if the given item implements the
     *   necessary interface.
     */
    __supportsInvalid : function(formItem) {
      var clazz = formItem.constructor;
      return qx.Class.hasInterface(clazz, qx.ui.form.IForm);
    },


    /**
     * Returns true, if the given item implements the
     * {@link qx.ui.core.ISingleSelection} interface.
     *
     * @param formItem {qx.core.Object} The item to check.
     * @return {Boolean} true, if the given item implements the
     *   necessary interface.
     */
    __supportsSingleSelection : function(formItem) {
      var clazz = formItem.constructor;
      return qx.Class.hasInterface(clazz, qx.ui.core.ISingleSelection);
    },


    /**
     * Internal setter for the valid member. It generates the event if
     * necessary and stores the new value
     *
     * @param value {Boolean|null} The new valid value of the manager.
     */
    __setValid: function(value) {
      var oldValue = this.__valid;
      this.__valid = value;
      // check for the change event
      if (oldValue != value) {
        this.fireDataEvent("changeValid", value, oldValue);
      }
    },


    /**
     * Returns the valid state of the manager.
     *
     * @return {Boolean|null} The valid state of the manager.
     */
    getValid: function() {
      return this.__valid;
    },


    /**
     * Returns the valid state of the manager.
     *
     * @return {Boolean|null} The valid state of the manager.
     */
    isValid: function() {
      return this.getValid();
    },


    /**
     * Returns an array of all invalid messages of the invalid form items and
     * the form manager itself.
     *
     * @return {String[]} All invalid messages.
     */
    getInvalidMessages: function() {
      var messages = [];
      // combine the messages of all form items
      for (var i = 0; i < this.__formItems.length; i++) {
        var formItem = this.__formItems[i].item;
        if (!formItem.getValid()) {
          messages.push(formItem.getInvalidMessage());
        }
      }
      // add the forms fail message
      if (this.getInvalidMessage() != "") {
        messages.push(this.getInvalidMessage());
      }

      return messages;
    },


    /**
     * Selects invalid form items
     *
     * @return {Array} invalid form items
     */
    getInvalidFormItems : function() {
      var res = [];
      for (var i = 0; i < this.__formItems.length; i++) {
        var formItem = this.__formItems[i].item;
        if (!formItem.getValid()) {
          res.push(formItem);
        }
      }

      return res;
    },


    /**
     * Resets the validator.
     */
    reset: function() {
      // reset all form items
      for (var i = 0; i < this.__formItems.length; i++) {
        var dataEntry = this.__formItems[i];
        // set the field to valid
        dataEntry.item.setValid(true);
      }
      // set the manager to its initial valid value
      this.__valid = null;
    },


    /**
     * Internal helper method to set the given item to valid for asynchronous
     * validation calls. This indirection is used to determinate if the
     * validation process is completed or if other asynchronous validators
     * are still validating. {@link #__checkValidationComplete} checks if the
     * validation is complete and will be called at the end of this method.
     *
     * @param formItem {qx.ui.core.Widget} The form item to set the valid state.
     * @param valid {Boolean} The valid state for the form item.
     *
     * @internal
     */
    setItemValid: function(formItem, valid) {
      // store the result
      this.__asyncResults[formItem.toHashCode()] = valid;
      formItem.setValid(valid);
      this.__checkValidationComplete();
    },


    /**
     * Internal helper method to set the form manager to valid for asynchronous
     * validation calls. This indirection is used to determinate if the
     * validation process is completed or if other asynchronous validators
     * are still validating. {@link #__checkValidationComplete} checks if the
     * validation is complete and will be called at the end of this method.
     *
     * @param valid {Boolean} The valid state for the form manager.
     *
     * @internal
     */
    setFormValid : function(valid) {
      this.__asyncResults[this.toHashCode()] = valid;
      this.__checkValidationComplete();
    },


    /**
     * Checks if all asynchronous validators have validated so the result
     * is final and the {@link #complete} event can be fired. If that's not
     * the case, nothing will happen in the method.
     */
    __checkValidationComplete : function() {
      var valid = this.__syncValid;

      // check if all async validators are done
      for (var hash in this.__asyncResults) {
        var currentResult = this.__asyncResults[hash];
        valid = currentResult && valid;
        // the validation is not done so just do nothing
        if (currentResult == null) {
          return;
        }
      }
      // set the actual valid state of the manager
      this.__setValid(valid);
      // reset the results
      this.__asyncResults = {};
      // fire the complete event (no entry in the results with null)
      this.fireEvent("complete");
    }
  },


  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */
  destruct : function()
  {
    this.__formItems = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */
/**
 * This class is responsible for validation in all asynchronous cases and
 * should always be used with {@link qx.ui.form.validation.Manager}.
 *
 *
 * It acts like a wrapper for asynchronous validation functions. These
 * validation function must be set in the constructor. The form manager will
 * invoke the validation and the validator function will be called with two
 * arguments:
 * <ul>
 *  <li>asyncValidator: A reference to the corresponding validator.</li>
 *  <li>value: The value of the assigned input field.</li>
 * </ul>
 * These two parameters are needed to set the validation status of the current
 * validator. {@link #setValid} is responsible for doing that.
 *
 *
 * *Warning:* Instances of this class can only be used with one input
 * field at a time. Multi usage is not supported!
 *
 * *Warning:* Calling {@link #setValid} synchronously does not work. If you
 * have an synchronous validator, please check
 * {@link qx.ui.form.validation.Manager#add}. If you have both cases, you have
 * to wrap the synchronous call in a timeout to make it asychronous.
 */
qx.Class.define("qx.ui.form.validation.AsyncValidator",
{
  extend : qx.core.Object,

  /**
   * @param validator {Function} The validator function, which has to be
   *   asynchronous.
   */
  construct : function(validator)
  {
    this.base(arguments);
    // save the validator function
    this.__validatorFunction = validator;
  },

  members :
  {
    __validatorFunction : null,
    __item : null,
    __manager : null,
    __usedForForm : null,

    /**
     * The validate function should only be called by
     * {@link qx.ui.form.validation.Manager}.
     *
     * It stores the given information and calls the validation function set in
     * the constructor. The method is used for form fields only. Validating a
     * form itself will be invokes with {@link #validateForm}.
     *
     * @param item {qx.ui.core.Widget} The form item which should be validated.
     * @param value {var} The value of the form item.
     * @param manager {qx.ui.form.validation.Manager} A reference to the form
     *   manager.
     * @param context {var?null} The context of the validator.
     *
     * @internal
     */
    validate: function(item, value, manager, context) {
      // mark as item validator
      this.__usedForForm = false;
      // store the item and the manager
      this.__item = item;
      this.__manager = manager;
      // invoke the user set validator function
      this.__validatorFunction.call(context || this, this, value);
    },


    /**
     * The validateForm function should only be called by
     * {@link qx.ui.form.validation.Manager}.
     *
     * It stores the given information and calls the validation function set in
     * the constructor. The method is used for forms only. Validating a
     * form item will be invokes with {@link #validate}.
     *
     * @param items {qx.ui.core.Widget[]} All form items of the form manager.
     * @param manager {qx.ui.form.validation.Manager} A reference to the form
     *   manager.
     * @param context {var?null} The context of the validator.
     *
     * @internal
     */
    validateForm : function(items, manager, context) {
      this.__usedForForm = true;
      this.__manager = manager;
      this.__validatorFunction.call(context, items, this);
    },


    /**
     * This method should be called within the asynchronous callback to tell the
     * validator the result of the validation.
     *
     * @param valid {Boolean} The boolean state of the validation.
     * @param message {String?} The invalidMessage of the validation.
     */
    setValid: function(valid, message) {
      // valid processing
      if (this.__usedForForm) {
        // message processing
        if (message !== undefined) {
          this.__manager.setInvalidMessage(message);
        }
        this.__manager.setFormValid(valid);
      } else {
        // message processing
        if (message !== undefined) {
          this.__item.setInvalidMessage(message);
        }
        this.__manager.setItemValid(this.__item, valid);
      }
    }
  },


  /*
   *****************************************************************************
      DESTRUCT
   *****************************************************************************
   */

  destruct : function() {
    this.__manager = this.__item = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */
/**
 * The resetter is responsible for managing a set of items and resetting these
 * items on a {@link #reset} call. It can handle all form items supplying a
 * value property and all widgets implementing the single selection linked list
 * or select box.
 */
qx.Class.define("qx.ui.form.Resetter",
{
  extend : qx.core.Object,


  construct : function()
  {
    this.base(arguments);

    this.__items = [];
  },

  members :
  {
    __items : null,

    /**
     * Adding a widget to the reseter will get its current value and store
     * it for resetting. To access the value, the given item needs to specify
     * a value property or implement the {@link qx.ui.core.ISingleSelection}
     * interface.
     *
     * @param item {qx.ui.core.Widget} The widget which should be added.
     */
    add : function(item) {
      // check the init values
      if (this._supportsValue(item)) {
        var init = item.getValue();
      } else if (this.__supportsSingleSelection(item)) {
        var init = item.getSelection();
      } else if (this.__supportsDataBindingSelection(item)) {
        var init = item.getSelection().concat();
      } else {
        throw new Error("Item " + item + " not supported for reseting.");
      }
      // store the item and its init value
      this.__items.push({item: item, init: init});
    },


    /**
     * Resets all added form items to their initial value. The initial value
     * is the value in the widget during the {@link #add}.
     */
    reset: function() {
      // reset all form items
      for (var i = 0; i < this.__items.length; i++) {
        var dataEntry = this.__items[i];
        // set the init value
        this.__setItem(dataEntry.item, dataEntry.init);
      }
    },


    /**
     * Resets a single given item. The item has to be added to the resetter
     * instance before. Otherwise, an error is thrown.
     *
     * @param item {qx.ui.core.Widget} The widget, which should be resetted.
     */
    resetItem : function(item)
    {
      // get the init value
      var init;
      for (var i = 0; i < this.__items.length; i++) {
        var dataEntry = this.__items[i];
        if (dataEntry.item === item) {
          init = dataEntry.init;
          break;
        }
      };

      // check for the available init value
      if (init === undefined) {
        throw new Error("The given item has not been added.");
      }

      this.__setItem(item, init);
    },


    /**
     * Internal helper for setting an item to a given init value. It checks
     * for the supported APIs and uses the fitting API.
     *
     * @param item {qx.ui.core.Widget} The item to reset.
     * @param init {var} The value to set.
     */
    __setItem : function(item, init)
    {
      // set the init value
      if (this._supportsValue(item)) {
        item.setValue(init);
      } else if (
        this.__supportsSingleSelection(item) ||
        this.__supportsDataBindingSelection(item)
      ) {
        item.setSelection(init);
      }
    },


    /**
     * Takes the current values of all added items and uses these values as
     * init values for resetting.
     */
    redefine: function() {
      // go threw all added items
      for (var i = 0; i < this.__items.length; i++) {
        var item = this.__items[i].item;
        // set the new init value for the item
        this.__items[i].init = this.__getCurrentValue(item);
      }
    },


    /**
     * Takes the current value of the given item and stores this value as init
     * value for resetting.
     *
     * @param item {qx.ui.core.Widget} The item to redefine.
     */
    redefineItem : function(item)
    {
      // get the data entry
      var dataEntry;
      for (var i = 0; i < this.__items.length; i++) {
        if (this.__items[i].item === item) {
          dataEntry = this.__items[i];
          break;
        }
      };

      // check for the available init value
      if (dataEntry === undefined) {
        throw new Error("The given item has not been added.");
      }

      // set the new init value for the item
      dataEntry.init = this.__getCurrentValue(dataEntry.item);
    },


    /**
     * Internal helper top access the value of a given item.
     *
     * @param item {qx.ui.core.Widget} The item to access.
     * @return {var} The item's value
     */
    __getCurrentValue : function(item)
    {
      if (this._supportsValue(item)) {
        return item.getValue();
      } else if (
        this.__supportsSingleSelection(item) ||
        this.__supportsDataBindingSelection(item)
      ) {
        return item.getSelection();
      }
    },


    /**
     * Returns true, if the given item implements the
     * {@link qx.ui.core.ISingleSelection} interface.
     *
     * @param formItem {qx.core.Object} The item to check.
     * @return {Boolean} true, if the given item implements the
     *   necessary interface.
     */
    __supportsSingleSelection : function(formItem) {
      var clazz = formItem.constructor;
      return qx.Class.hasInterface(clazz, qx.ui.core.ISingleSelection);
    },


    /**
     * Returns true, if the given item implements the
     * {@link qx.data.controller.ISelection} interface.
     *
     * @param formItem {qx.core.Object} The item to check.
     * @return {Boolean} true, if the given item implements the
     *   necessary interface.
     */
    __supportsDataBindingSelection : function(formItem) {
      var clazz = formItem.constructor;
      return qx.Class.hasInterface(clazz, qx.data.controller.ISelection);
    },


    /**
     * Returns true, if the value property is supplied by the form item.
     *
     * @param formItem {qx.core.Object} The item to check.
     * @return {Boolean} true, if the given item implements the
     *   necessary interface.
     */
    _supportsValue : function(formItem) {
      var clazz = formItem.constructor;
      return (
        qx.Class.hasInterface(clazz, qx.ui.form.IBooleanForm) ||
        qx.Class.hasInterface(clazz, qx.ui.form.IColorForm) ||
        qx.Class.hasInterface(clazz, qx.ui.form.IDateForm) ||
        qx.Class.hasInterface(clazz, qx.ui.form.INumberForm) ||
        qx.Class.hasInterface(clazz, qx.ui.form.IStringForm)
      );
    }
  },


  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */
  destruct : function()
  {
    // holding references to widgets --> must set to null
    this.__items = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Form interface for all form widgets which have boolean as their primary
 * data type like a colorchooser.
 */
qx.Interface.define("qx.ui.form.IColorForm",
{
  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events :
  {
    /** Fired when the value was modified */
    "changeValue" : "qx.event.type.Data"
  },



  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
    ---------------------------------------------------------------------------
      VALUE PROPERTY
    ---------------------------------------------------------------------------
    */

    /**
     * Sets the element's value.
     *
     * @param value {Color|null} The new value of the element.
     */
    setValue : function(value) {
      return arguments.length == 1;
    },


    /**
     * Resets the element's value to its initial value.
     */
    resetValue : function() {},


    /**
     * The element's user set value.
     *
     * @return {Color|null} The value.
     */
    getValue : function() {}
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)

************************************************************************ */

/**
 * A password input field, which hides the entered text.
 */
qx.Class.define("qx.ui.form.PasswordField",
{
  extend : qx.ui.form.TextField,

  members :
  {
    // overridden
    _createInputElement : function() {
      return new qx.html.Input("password");
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2009 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Double column renderer for {@link qx.ui.form.Form}.
 */
qx.Class.define("qx.ui.form.renderer.Double",
{
  extend : qx.ui.form.renderer.AbstractRenderer,


  construct : function(form)
  {
    var layout = new qx.ui.layout.Grid();
    layout.setSpacing(6);
    layout.setColumnAlign(0, "right", "top");
    layout.setColumnAlign(1, "left", "top");
    layout.setColumnAlign(2, "right", "top");
    layout.setColumnAlign(3, "left", "top");
    this._setLayout(layout);

    this.base(arguments, form);
  },


  members :
  {
    _row : 0,
    _buttonRow : null,

    /**
     * Add a group of form items with the corresponding names. The names are
     * displayed as label.
     * The title is optional and is used as grouping for the given form
     * items.
     *
     * @param items {qx.ui.core.Widget[]} An array of form items to render.
     * @param names {String[]} An array of names for the form items.
     * @param title {String?} A title of the group you are adding.
     */
    addItems : function(items, names, title) {
      // add the header
      if (title != null) {
        this._add(
          this._createHeader(title), {row: this._row, column: 0, colSpan: 4}
        );
        this._row++;
      }

      // add the items
      for (var i = 0; i < items.length; i++) {
        var label = this._createLabel(names[i], items[i]);
        this._add(label, {row: this._row, column: (i * 2) % 4});
        var item = items[i];
        label.setBuddy(item);

        this._connectVisibility(item, label);

        this._add(item, {row: this._row, column: ((i * 2) % 4) + 1});
        if (i % 2 == 1) {
          this._row++;
        }

        // store the names for translation
        if (qx.core.Environment.get("qx.dynlocale")) {
          this._names.push({name: names[i], label: label, item: items[i]});
        }
      }

      if (i % 2 == 1) {
        this._row++;
      }
    },

    /**
     * Adds a button the form renderer. All buttons will be added in a
     * single row at the bottom of the form.
     *
     * @param button {qx.ui.form.Button} The button to add.
     */
    addButton : function(button) {
      if (this._buttonRow == null) {
        // create button row
        this._buttonRow = new qx.ui.container.Composite();
        this._buttonRow.setMarginTop(5);
        var hbox = new qx.ui.layout.HBox();
        hbox.setAlignX("right");
        hbox.setSpacing(5);
        this._buttonRow.setLayout(hbox);
        // add the button row
        this._add(this._buttonRow, {row: this._row, column: 0, colSpan: 4});
        // increase the row
        this._row++;
      }

      // add the button
      this._buttonRow.add(button);
    },


    /**
     * Returns the set layout for configuration.
     *
     * @return {qx.ui.layout.Grid} The grid layout of the widget.
     */
    getLayout : function() {
      return this._getLayout();
    },


    /**
     * Creates a label for the given form item.
     *
     * @param name {String} The content of the label without the
     *   trailing * and :
     * @param item {qx.ui.core.Widget} The item, which has the required state.
     * @return {qx.ui.basic.Label} The label for the given item.
     */
    _createLabel : function(name, item) {
      var label = new qx.ui.basic.Label(this._createLabelText(name, item));
      // store lables for disposal
      this._labels.push(label);
      label.setRich(true);
      return label;
    },


    /**
     * Creates a header label for the form groups.
     *
     * @param title {String} Creates a header label.
     * @return {qx.ui.basic.Label} The header for the form groups.
     */
    _createHeader : function(title) {
      var header = new qx.ui.basic.Label(title);
      // store lables for disposal
      this._labels.push(header);
      header.setFont("bold");
      if (this._row != 0) {
        header.setMarginTop(10);
      }
      header.setAlignX("left");
      return header;
    }
  },


  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */
  destruct : function() {
    // first, remove all buttons from the botton row because they
    // should not be disposed
    if (this._buttonRow) {
      this._buttonRow.removeAll();
      this._disposeObjects("_buttonRow");
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)
     * Fabian Jakobs (fjakobs)
     * Jonathan Weiß (jonathan_rass)

************************************************************************ */

/**
 * Table
 *
 * A detailed description can be found in the package description
 * {@link qx.ui.table}.
 *
 * @childControl statusbar {qx.ui.basic.Label} label to show the status of the table
 * @childControl column-button {qx.ui.table.columnmenu.Button} button to open the column menu
 */
qx.Class.define("qx.ui.table.Table",
{
  extend : qx.ui.core.Widget,
  include : qx.ui.core.MDragDropScrolling,



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param tableModel {qx.ui.table.ITableModel ? null}
   *   The table model to read the data from.
   *
   * @param custom {Map ? null}
   *   A map provided to override the various supplemental classes allocated
   *   within this constructor.  Each property must be a function which
   *   returns an object instance, as indicated by shown the defaults listed
   *   here:
   *
   *   <dl>
   *     <dt>initiallyHiddenColumns</dt>
   *       <dd>
   *         {Array?}
   *         A list of column numbers that should be initially invisible. Any
   *         column not mentioned will be initially visible, and if no array
   *         is provided, all columns will be initially visible.
   *       </dd>
   *     <dt>selectionManager</dt>
   *       <dd><pre class='javascript'>
   *         function(obj)
   *         {
   *           return new qx.ui.table.selection.Manager(obj);
   *         }
   *       </pre></dd>
   *     <dt>selectionModel</dt>
   *       <dd><pre class='javascript'>
   *         function(obj)
   *         {
   *           return new qx.ui.table.selection.Model(obj);
   *         }
   *       </pre></dd>
   *     <dt>tableColumnModel</dt>
   *       <dd><pre class='javascript'>
   *         function(obj)
   *         {
   *           return new qx.ui.table.columnmodel.Basic(obj);
   *         }
   *       </pre></dd>
   *     <dt>tablePaneModel</dt>
   *       <dd><pre class='javascript'>
   *         function(obj)
   *         {
   *           return new qx.ui.table.pane.Model(obj);
   *         }
   *       </pre></dd>
   *     <dt>tablePane</dt>
   *       <dd><pre class='javascript'>
   *         function(obj)
   *         {
   *           return new qx.ui.table.pane.Pane(obj);
   *         }
   *       </pre></dd>
   *     <dt>tablePaneHeader</dt>
   *       <dd><pre class='javascript'>
   *         function(obj)
   *         {
   *           return new qx.ui.table.pane.Header(obj);
   *         }
   *       </pre></dd>
   *     <dt>tablePaneScroller</dt>
   *       <dd><pre class='javascript'>
   *         function(obj)
   *         {
   *           return new qx.ui.table.pane.Scroller(obj);
   *         }
   *       </pre></dd>
   *     <dt>tablePaneModel</dt>
   *       <dd><pre class='javascript'>
   *         function(obj)
   *         {
   *           return new qx.ui.table.pane.Model(obj);
   *         }
   *       </pre></dd>
   *     <dt>columnMenu</dt>
   *       <dd><pre class='javascript'>
   *         function()
   *         {
   *           return new qx.ui.table.columnmenu.Button();
   *         }
   *       </pre></dd>
   *   </dl>
   */
  construct : function(tableModel, custom)
  {
    this.base(arguments);
    //
    // Use default objects if custom objects are not specified
    //
    if (!custom) {
      custom = { };
    }

    if (custom.initiallyHiddenColumns) {
      this.setInitiallyHiddenColumns(custom.initiallyHiddenColumns);
    }

    if (custom.selectionManager) {
      this.setNewSelectionManager(custom.selectionManager);
    }

    if (custom.selectionModel) {
      this.setNewSelectionModel(custom.selectionModel);
    }

    if (custom.tableColumnModel) {
      this.setNewTableColumnModel(custom.tableColumnModel);
    }

    if (custom.tablePane) {
      this.setNewTablePane(custom.tablePane);
    }

    if (custom.tablePaneHeader) {
      this.setNewTablePaneHeader(custom.tablePaneHeader);
    }

    if (custom.tablePaneScroller) {
      this.setNewTablePaneScroller(custom.tablePaneScroller);
    }

    if (custom.tablePaneModel) {
      this.setNewTablePaneModel(custom.tablePaneModel);
    }

    if (custom.columnMenu) {
      this.setNewColumnMenu(custom.columnMenu);
    }

    this._setLayout(new qx.ui.layout.VBox());

    // Create the child widgets
    this.__scrollerParent = new qx.ui.container.Composite(new qx.ui.layout.HBox());
    this._add(this.__scrollerParent, {flex: 1});

    // Allocate a default data row renderer
    this.setDataRowRenderer(new qx.ui.table.rowrenderer.Default(this));

    // Create the models
    this.__selectionManager = this.getNewSelectionManager()(this);
    this.setSelectionModel(this.getNewSelectionModel()(this));
    this.setTableModel(tableModel || this.getEmptyTableModel());

    // create the main meta column
    this.setMetaColumnCounts([ -1 ]);

    // Make focusable
    this.setTabIndex(1);
    this.addListener("keypress", this._onKeyPress);
    this.addListener("focus", this._onFocusChanged);
    this.addListener("blur", this._onFocusChanged);

    // attach the resize listener to the last child of the layout. This
    // ensures that all other children are laid out before
    var spacer = new qx.ui.core.Widget().set({
      height: 0
    });
    this._add(spacer);
    spacer.addListener("resize", this._onResize, this);

    this.__focusedCol = null;
    this.__focusedRow = null;

    // add an event listener which updates the table content on locale change
    if (qx.core.Environment.get("qx.dynlocale")) {
      qx.locale.Manager.getInstance().addListener("changeLocale", this._onChangeLocale, this);
    }

    this.initStatusBarVisible();

    // If the table model has an init() method...
    tableModel = this.getTableModel();
    if (tableModel.init && typeof(tableModel.init) == "function")
    {
      // ... then call it now to allow the table model to affect table
      // properties.
      tableModel.init(this);
    }
  },




  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events :
  {
    /**
     * Dispatched before adding the column list to the column visibility menu.
     * The event data is a map with two properties: table and menu.  Listeners
     * may add additional items to the menu, which appear at the top of the
     * menu.
     */
    "columnVisibilityMenuCreateStart" : "qx.event.type.Data",

    /**
     * Dispatched after adding the column list to the column visibility menu.
     * The event data is a map with two properties: table and menu.  Listeners
     * may add additional items to the menu, which appear at the bottom of the
     * menu.
     */
    "columnVisibilityMenuCreateEnd" : "qx.event.type.Data",

     /**
      * Dispatched when the width of the table has changed.
      */
    "tableWidthChanged" : "qx.event.type.Event",

    /**
     * Dispatched when updating scrollbars discovers that a vertical scrollbar
     * is needed when it previously was not, or vice versa.  The data is a
     * boolean indicating whether a vertical scrollbar is now being used.
     */
    "verticalScrollBarChanged" : "qx.event.type.Data",

    /**
     * Dispatched when a data cell has been clicked.
     */
    "cellClick" : "qx.ui.table.pane.CellEvent",

    /**
     * Dispatched when a data cell has been clicked.
     */
    "cellDblclick" : "qx.ui.table.pane.CellEvent",

    /**
     * Dispatched when the context menu is needed in a data cell
     */
    "cellContextmenu" : "qx.ui.table.pane.CellEvent",

    /**
     * Dispatched after a cell editor is flushed.
     *
     * The data is a map containing this properties:
     * <ul>
     *   <li>row</li>
     *   <li>col</li>
     *   <li>value</li>
     *   <li>oldValue</li>
     * </ul>
     */
    "dataEdited" : "qx.event.type.Data"
  },



  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

  statics :
  {
    /** Events that must be redirected to the scrollers. */
    __redirectEvents : { cellClick: 1, cellDblclick: 1, cellContextmenu: 1 }
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    appearance :
    {
      refine : true,
      init : "table"
    },


    focusable :
    {
      refine : true,
      init : true
    },


    minWidth :
    {
      refine : true,
      init : 50
    },

    /**
     * The list of columns that are initially hidden. This property is set by
     * the constructor, from the value received in
     * custom.initiallyHiddenColumns, and is only used when a column model is
     * initialized. It can be of great benefit in tables with numerous columns
     * where most are not initially visible. The process of creating the
     * headers for all of the columns, only to have those columns discarded
     * shortly thereafter when setColumnVisibility(false) is called, is a
     * waste of (significant, in some browsers) time. Specifying the
     * non-visible columns at constructor time can therefore avoid the initial
     * creation of all of those superfluous widgets.
     */
    initiallyHiddenColumns :
    {
      init : null
    },

    /**
     * Whether the widget contains content which may be selected by the user.
     *
     * If the value set to <code>true</code> the native browser selection can
     * be used for text selection. But it is normally useful for
     * forms fields, longer texts/documents, editors, etc.
     *
     * Note: This has no effect on Table!
     */
    selectable :
    {
      refine : true,
      init : false
    },


    /** The selection model. */
    selectionModel :
    {
      check : "qx.ui.table.selection.Model",
      apply : "_applySelectionModel",
      event : "changeSelectionModel"
    },


    /** The table model. */
    tableModel :
    {
      check : "qx.ui.table.ITableModel",
      apply : "_applyTableModel",
      event : "changeTableModel"
    },


    /** The height of the table rows. */
    rowHeight :
    {
      check : "Number",
      init : 20,
      apply : "_applyRowHeight",
      event : "changeRowHeight",
      themeable : true
    },


    /**
     * Force line height to match row height.  May be disabled if cell
     * renderers being used wish to render multiple lines of data within a
     * cell.  (With the default setting, all but the first of multiple lines
     * of data will not be visible.)
     */
    forceLineHeight :
    {
      check : "Boolean",
      init  : true
    },


    /**
     *  Whether the header cells are visible. When setting this to false,
     *  you'll likely also want to set the {#columnVisibilityButtonVisible}
     *  property to false as well, to entirely remove the header row.
     */
    headerCellsVisible :
    {
      check : "Boolean",
      init : true,
      apply : "_applyHeaderCellsVisible",
      themeable : true
    },


    /** The height of the header cells. */
    headerCellHeight :
    {
      check : "Integer",
      init : 16,
      apply : "_applyHeaderCellHeight",
      event : "changeHeaderCellHeight",
      nullable : true,
      themeable : true
    },


    /** Whether to show the status bar */
    statusBarVisible :
    {
      check : "Boolean",
      init : true,
      apply : "_applyStatusBarVisible"
    },


    /** The Statusbartext, set it, if you want some more Information */
    additionalStatusBarText :
    {
      nullable : true,
      init : null,
      apply : "_applyAdditionalStatusBarText"
    },


    /** Whether to show the column visibility button */
    columnVisibilityButtonVisible :
    {
      check : "Boolean",
      init : true,
      apply : "_applyColumnVisibilityButtonVisible",
      themeable : true
    },


    /**
     * @type {Integer[]} The number of columns per meta column. If the last array entry is -1,
     * this meta column will get the remaining columns.
     */
    metaColumnCounts :
    {
      check : "Object",
      apply : "_applyMetaColumnCounts"
    },


    /**
     * Whether the focus should moved when the mouse is moved over a cell. If false
     * the focus is only moved on mouse clicks.
     */
    focusCellOnMouseMove :
    {
      check : "Boolean",
      init : false,
      apply : "_applyFocusCellOnMouseMove"
    },

    /**
     * Whether row focus change by keyboard also modifies selection
     */
    rowFocusChangeModifiesSelection :
    {
      check : "Boolean",
      init : true
    },

    /**
     * Whether the cell focus indicator should be shown
     */
    showCellFocusIndicator :
    {
      check : "Boolean",
      init : true,
      apply : "_applyShowCellFocusIndicator"
    },

    /**
     * By default, the "cellContextmenu" event is fired only when a data cell
     * is right-clicked. It is not fired when a right-click occurs in the
     * empty area of the table below the last data row. By turning on this
     * property, "cellContextMenu" events will also be generated when a
     * right-click occurs in that empty area. In such a case, row identifier
     * in the event data will be null, so event handlers can check (row ===
     * null) to handle this case.
     */
    contextMenuFromDataCellsOnly :
    {
      check : "Boolean",
      init : true,
      apply : "_applyContextMenuFromDataCellsOnly"
    },

    /**
     * Whether the table should keep the first visible row complete. If set to false,
     * the first row may be rendered partial, depending on the vertical scroll value.
     */
    keepFirstVisibleRowComplete :
    {
      check : "Boolean",
      init : true,
      apply : "_applyKeepFirstVisibleRowComplete"
    },


    /**
     * Whether the table cells should be updated when only the selection or the
     * focus changed. This slows down the table update but allows to react on a
     * changed selection or a changed focus in a cell renderer.
     */
    alwaysUpdateCells :
    {
      check : "Boolean",
      init : false
    },


    /**
     * Whether to reset the selection when a header cell is clicked. Since
     * most data models do not have provisions to retain a selection after
     * sorting, the default is to reset the selection in this case. Some data
     * models, however, do have the capability to retain the selection, so
     * when using those, this property should be set to false.
     */
    resetSelectionOnHeaderClick :
    {
      check : "Boolean",
      init : true,
      apply : "_applyResetSelectionOnHeaderClick"
    },


    /** The renderer to use for styling the rows. */
    dataRowRenderer :
    {
      check : "qx.ui.table.IRowRenderer",
      init : null,
      nullable : true,
      event : "changeDataRowRenderer"
    },


    /**
     * A function to call when before modal cell editor is opened.
     *
     * @signature function(cellEditor, cellInfo)
     *
     * @param cellEditor {qx.ui.window.Window}
     *   The modal window which has been created for this cell editor
     *
     * @param cellInfo {Map}
     *   Information about the cell for which this cell editor was created.
     *   It contains the following properties:
     *       col, row, xPos, value
     *
     */
    modalCellEditorPreOpenFunction :
    {
      check : "Function",
      init : null,
      nullable : true
    },


    /**
     * A function to instantiate a new column menu button.
     */
    newColumnMenu :
    {
      check : "Function",
      init  : function() {
        return new qx.ui.table.columnmenu.Button();
      }
    },


    /**
     * A function to instantiate a selection manager.  this allows subclasses of
     * Table to subclass this internal class.  To take effect, this property must
     * be set before calling the Table constructor.
     */
    newSelectionManager :
    {
      check : "Function",
      init : function(obj) {
        return new qx.ui.table.selection.Manager(obj);
      }
    },


    /**
     * A function to instantiate a selection model.  this allows subclasses of
     * Table to subclass this internal class.  To take effect, this property must
     * be set before calling the Table constructor.
     */
    newSelectionModel :
    {
      check : "Function",
      init : function(obj) {
        return new qx.ui.table.selection.Model(obj);
      }
    },


    /**
     * A function to instantiate a table column model.  This allows subclasses
     * of Table to subclass this internal class.  To take effect, this
     * property must be set before calling the Table constructor.
     */
    newTableColumnModel :
    {
      check : "Function",
      init : function(table) {
        return new qx.ui.table.columnmodel.Basic(table);
      }
    },


    /**
     * A function to instantiate a table pane.  this allows subclasses of
     * Table to subclass this internal class.  To take effect, this property
     * must be set before calling the Table constructor.
     */
    newTablePane :
    {
      check : "Function",
      init : function(obj) {
        return new qx.ui.table.pane.Pane(obj);
      }
    },


    /**
     * A function to instantiate a table pane.  this allows subclasses of
     * Table to subclass this internal class.  To take effect, this property
     * must be set before calling the Table constructor.
     */
    newTablePaneHeader :
    {
      check : "Function",
      init : function(obj) {
        return new qx.ui.table.pane.Header(obj);
      }
    },


    /**
     * A function to instantiate a table pane scroller.  this allows
     * subclasses of Table to subclass this internal class.  To take effect,
     * this property must be set before calling the Table constructor.
     */
    newTablePaneScroller :
    {
      check : "Function",
      init : function(obj) {
        return new qx.ui.table.pane.Scroller(obj);
      }
    },


    /**
     * A function to instantiate a table pane model.  this allows subclasses
     * of Table to subclass this internal class.  To take effect, this
     * property must be set before calling the Table constructor.
     */
    newTablePaneModel :
    {
      check : "Function",
      init : function(columnModel) {
        return new qx.ui.table.pane.Model(columnModel);
      }
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __focusedCol : null,
    __focusedRow : null,

    __scrollerParent : null,

    __selectionManager : null,

    __additionalStatusBarText : null,
    __lastRowCount : null,
    __internalChange : null,

    __columnMenuButtons : null,
    __columnModel : null,
    __emptyTableModel : null,

    __hadVerticalScrollBar : null,

    __timer : null,


    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
      case "statusbar":
        control = new qx.ui.basic.Label();
        control.set(
          {
            allowGrowX: true
          });
        this._add(control);
        break;

      case "column-button":
        control = this.getNewColumnMenu()();
        control.set({
          focusable : false
        });

        // Create the initial menu too
        var menu = control.factory("menu", { table : this });

        // Add a listener to initialize the column menu when it becomes visible
        menu.addListener(
          "appear",
          this._initColumnMenu,
          this
        );

        break;
      }

      return control || this.base(arguments, id);
    },



    // property modifier
    _applySelectionModel : function(value, old)
    {
      this.__selectionManager.setSelectionModel(value);

      if (old != null) {
        old.removeListener("changeSelection", this._onSelectionChanged, this);
      }

      value.addListener("changeSelection", this._onSelectionChanged, this);
    },


    // property modifier
    _applyRowHeight : function(value, old)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].updateVerScrollBarMaximum();
      }
    },


    // property modifier
    _applyHeaderCellsVisible : function(value, old)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        if (value) {
          scrollerArr[i]._showChildControl("header");
        } else {
          scrollerArr[i]._excludeChildControl("header");
        }
      }
      // also hide the column visibility button
      if(this.getColumnVisibilityButtonVisible()) {
        this._applyColumnVisibilityButtonVisible(value);
      }
    },


    // property modifier
    _applyHeaderCellHeight : function(value, old)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].getHeader().setHeight(value);
      }
    },


    /**
     * Get an empty table model instance to use for this table. Use this table
     * to configure the table with no table model.
     *
     * @return {qx.ui.table.ITableModel} The empty table model
     */
    getEmptyTableModel : function()
    {
      if (!this.__emptyTableModel)
      {
        this.__emptyTableModel = new qx.ui.table.model.Simple();
        this.__emptyTableModel.setColumns([]);
        this.__emptyTableModel.setData([]);
      }
      return this.__emptyTableModel;
    },


    // property modifier
    _applyTableModel : function(value, old)
    {
      this.getTableColumnModel().init(value.getColumnCount(), this);

      if (old != null)
      {
        old.removeListener(
          "metaDataChanged",
          this._onTableModelMetaDataChanged, this
        );

        old.removeListener(
          "dataChanged",
          this._onTableModelDataChanged,
          this);
      }

      value.addListener(
        "metaDataChanged",
        this._onTableModelMetaDataChanged, this
      );

      value.addListener(
        "dataChanged",
        this._onTableModelDataChanged,
        this);

      // Update the status bar
      this._updateStatusBar();

      this._updateTableData(
        0, value.getRowCount(),
        0, value.getColumnCount()
      );
      this._onTableModelMetaDataChanged();

      // If the table model has an init() method, call it. We don't, however,
      // call it if this is the initial setting of the table model, as the
      // scrollers are not yet initialized. In that case, the init method is
      // called explicitly by the Table constructor.
      if (old && value.init && typeof(value.init) == "function")
      {
        value.init(this);
      }
    },


    /**
     * Get the The table column model.
     *
     * @return {qx.ui.table.columnmodel.Basic} The table's column model
     */
    getTableColumnModel : function()
    {
      if (!this.__columnModel)
      {
        var columnModel = this.__columnModel = this.getNewTableColumnModel()(this);

        columnModel.addListener("visibilityChanged", this._onColVisibilityChanged, this);
        columnModel.addListener("widthChanged", this._onColWidthChanged, this);
        columnModel.addListener("orderChanged", this._onColOrderChanged, this);

        // Get the current table model
        var tableModel = this.getTableModel();
        columnModel.init(tableModel.getColumnCount(), this);

        // Reset the table column model in each table pane model
        var scrollerArr = this._getPaneScrollerArr();

        for (var i=0; i<scrollerArr.length; i++)
        {
          var paneScroller = scrollerArr[i];
          var paneModel = paneScroller.getTablePaneModel();
          paneModel.setTableColumnModel(columnModel);
        }
      }
      return this.__columnModel;
    },


    // property modifier
    _applyStatusBarVisible : function(value, old)
    {
      if (value) {
        this._showChildControl("statusbar");
      } else {
        this._excludeChildControl("statusbar");
      }

      if (value) {
        this._updateStatusBar();
      }
    },


    // property modifier
    _applyAdditionalStatusBarText : function(value, old)
    {
      this.__additionalStatusBarText = value;
      this._updateStatusBar();
    },


    // property modifier
    _applyColumnVisibilityButtonVisible : function(value, old)
    {
      if (value) {
        this._showChildControl("column-button");
      } else {
        this._excludeChildControl("column-button");
      }
    },


    // property modifier
    _applyMetaColumnCounts : function(value, old)
    {
      var metaColumnCounts = value;
      var scrollerArr = this._getPaneScrollerArr();
      var handlers = { };

      if (value > old)
      {
        // Save event listeners on the redirected events so we can re-apply
        // them to new scrollers.
        var manager = qx.event.Registration.getManager(scrollerArr[0]);
        for (var evName in qx.ui.table.Table.__redirectEvents)
        {
          handlers[evName] = { };
          handlers[evName].capture = manager.getListeners(scrollerArr[0],
                                                          evName,
                                                          true);
          handlers[evName].bubble = manager.getListeners(scrollerArr[0],
                                                         evName,
                                                         false);
        }
      }

      // Remove the panes not needed any more
      this._cleanUpMetaColumns(metaColumnCounts.length);

      // Update the old panes
      var leftX = 0;

      for (var i=0; i<scrollerArr.length; i++)
      {
        var paneScroller = scrollerArr[i];
        var paneModel = paneScroller.getTablePaneModel();
        paneModel.setFirstColumnX(leftX);
        paneModel.setMaxColumnCount(metaColumnCounts[i]);
        leftX += metaColumnCounts[i];
      }

      // Add the new panes
      if (metaColumnCounts.length > scrollerArr.length)
      {
        var columnModel = this.getTableColumnModel();

        for (var i=scrollerArr.length; i<metaColumnCounts.length; i++)
        {
          var paneModel = this.getNewTablePaneModel()(columnModel);
          paneModel.setFirstColumnX(leftX);
          paneModel.setMaxColumnCount(metaColumnCounts[i]);
          leftX += metaColumnCounts[i];

          var paneScroller = this.getNewTablePaneScroller()(this);
          paneScroller.setTablePaneModel(paneModel);

          // Register event listener for vertical scrolling
          paneScroller.addListener("changeScrollY", this._onScrollY, this);

          // Apply redirected events to this new scroller
          for (evName in qx.ui.table.Table.__redirectEvents)
          {
            // On first setting of meta columns (constructing phase), there
            // are no handlers to deal with yet.
            if (! handlers[evName])
            {
              break;
            }

            if (handlers[evName].capture &&
                handlers[evName].capture.length > 0)
            {
              var capture = handlers[evName].capture;
              for (var j = 0; j < capture.length; j++)
              {
                // Determine what context to use.  If the context does not
                // exist, we assume that the context is this table.  If it
                // does exist and it equals the first pane scroller (from
                // which we retrieved the listeners) then set the context
                // to be this new pane scroller.  Otherwise leave the context
                // as it was set.
                var context = capture[j].context;
                if (! context)
                {
                  context = this;
                }
                else if (context == scrollerArr[0])
                {
                  context = paneScroller;
                }

                paneScroller.addListener(
                  evName,
                  capture[j].handler,
                  context,
                  true);
              }
            }

            if (handlers[evName].bubble &&
                handlers[evName].bubble.length > 0)
            {
              var bubble = handlers[evName].bubble;
              for (var j = 0; j < bubble.length; j++)
              {
                // Determine what context to use.  If the context does not
                // exist, we assume that the context is this table.  If it
                // does exist and it equals the first pane scroller (from
                // which we retrieved the listeners) then set the context
                // to be this new pane scroller.  Otherwise leave the context
                // as it was set.
                var context = bubble[j].context;
                if (! context)
                {
                  context = this;
                }
                else if (context == scrollerArr[0])
                {
                  context = paneScroller;
                }

                paneScroller.addListener(
                  evName,
                  bubble[j].handler,
                  context,
                  false);
              }
            }
          }

          // last meta column is flexible
          var flex = (i == metaColumnCounts.length - 1) ? 1 : 0;
          this.__scrollerParent.add(paneScroller, {flex: flex});
          scrollerArr = this._getPaneScrollerArr();
        }
      }

      // Update all meta columns
      for (var i=0; i<scrollerArr.length; i++)
      {
        var paneScroller = scrollerArr[i];
        var isLast = (i == (scrollerArr.length - 1));

        // Set the right header height
        paneScroller.getHeader().setHeight(this.getHeaderCellHeight());

        // Put the column visibility button in the top right corner of the last meta column
        paneScroller.setTopRightWidget(isLast ? this.getChildControl("column-button") : null);
      }

      if (!this.isColumnVisibilityButtonVisible()) {
        this._excludeChildControl("column-button");
      }

      this._updateScrollerWidths();
      this._updateScrollBarVisibility();
    },


    // property modifier
    _applyFocusCellOnMouseMove : function(value, old)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].setFocusCellOnMouseMove(value);
      }
    },


    // property modifier
    _applyShowCellFocusIndicator : function(value, old)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].setShowCellFocusIndicator(value);
      }
    },


    // property modifier
    _applyContextMenuFromDataCellsOnly : function(value, old)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].setContextMenuFromDataCellsOnly(value);
      }
    },


    // property modifier
    _applyKeepFirstVisibleRowComplete : function(value, old)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].onKeepFirstVisibleRowCompleteChanged();
      }
    },


    // property modifier
    _applyResetSelectionOnHeaderClick : function(value, old)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].setResetSelectionOnHeaderClick(value);
      }
    },


    /**
     * Returns the selection manager.
     *
     * @return {qx.ui.table.selection.Manager} the selection manager.
     */
    getSelectionManager : function() {
      return this.__selectionManager;
    },


    /**
     * Returns an array containing all TablePaneScrollers in this table.
     *
     * @return {qx.ui.table.pane.Scroller[]} all TablePaneScrollers in this table.
     */
    _getPaneScrollerArr : function() {
      return this.__scrollerParent.getChildren();
    },


    /**
     * Returns a TablePaneScroller of this table.
     *
     * @param metaColumn {Integer} the meta column to get the TablePaneScroller for.
     * @return {qx.ui.table.pane.Scroller} the qx.ui.table.pane.Scroller.
     */
    getPaneScroller : function(metaColumn) {
      return this._getPaneScrollerArr()[metaColumn];
    },


    /**
     * Cleans up the meta columns.
     *
     * @param fromMetaColumn {Integer} the first meta column to clean up. All following
     *      meta columns will be cleaned up, too. All previous meta columns will
     *      stay unchanged. If 0 all meta columns will be cleaned up.
     */
    _cleanUpMetaColumns : function(fromMetaColumn)
    {
      var scrollerArr = this._getPaneScrollerArr();

      if (scrollerArr != null)
      {
        for (var i=scrollerArr.length-1; i>=fromMetaColumn; i--)
        {
          scrollerArr[i].destroy();
        }
      }
    },


    /**
     * Event handler. Called when the locale has changed.
     *
     * @param evt {Event} the event.
     */
    _onChangeLocale : function(evt)
    {
      this.updateContent();
      this._updateStatusBar();
    },


    // overridden
    _onChangeTheme : function() {
      this.base(arguments);

      this.getDataRowRenderer().initThemeValues();
      this.updateContent();
      this._updateStatusBar();
    },


    /**
     * Event handler. Called when the selection has changed.
     *
     * @param evt {Map} the event.
     */
    _onSelectionChanged : function(evt)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].onSelectionChanged();
      }

      this._updateStatusBar();
    },


    /**
     * Event handler. Called when the table model meta data has changed.
     *
     * @param evt {Map} the event.
     */
    _onTableModelMetaDataChanged : function(evt)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].onTableModelMetaDataChanged();
      }

      this._updateStatusBar();
    },


    /**
     * Event handler. Called when the table model data has changed.
     *
     * @param evt {Map} the event.
     */
    _onTableModelDataChanged : function(evt)
    {
      var data = evt.getData();

      this._updateTableData(
        data.firstRow, data.lastRow,
        data.firstColumn, data.lastColumn,
        data.removeStart, data.removeCount
      );
    },

    /**
     * To update the table if the table model has changed and remove selection.
     *
     * @param firstRow {Integer} The index of the first row that has changed.
     * @param lastRow {Integer} The index of the last row that has changed.
     * @param firstColumn {Integer} The model index of the first column that has changed.
     * @param lastColumn {Integer} The model index of the last column that has changed.
     * @param removeStart {Integer ? null} The first index of the interval (including), to remove selection.
     * @param removeCount {Integer ? null} The count of the interval, to remove selection.
     */
    _updateTableData : function(firstRow, lastRow, firstColumn, lastColumn, removeStart, removeCount)
    {
      var scrollerArr = this._getPaneScrollerArr();

      // update selection if rows were removed
      if (removeCount) {
        this.getSelectionModel().removeSelectionInterval(removeStart, removeStart + removeCount);
        // remove focus if the focused row has been removed
        if (this.__focusedRow >= removeStart && this.__focusedRow < (removeStart + removeCount)) {
          this.setFocusedCell();
        }
      }

      for (var i=0; i<scrollerArr.length; i++)
      {
        scrollerArr[i].onTableModelDataChanged(
          firstRow, lastRow,
          firstColumn, lastColumn
        );
      }

      var rowCount = this.getTableModel().getRowCount();

      if (rowCount != this.__lastRowCount)
      {
        this.__lastRowCount = rowCount;

        this._updateScrollBarVisibility();
        this._updateStatusBar();
      }
    },


    /**
     * Event handler. Called when a TablePaneScroller has been scrolled vertically.
     *
     * @param evt {Map} the event.
     */
    _onScrollY : function(evt)
    {
      if (!this.__internalChange)
      {
        this.__internalChange = true;

        // Set the same scroll position to all meta columns
        var scrollerArr = this._getPaneScrollerArr();

        for (var i=0; i<scrollerArr.length; i++) {
          scrollerArr[i].setScrollY(evt.getData());
        }

        this.__internalChange = false;
      }
    },


    /**
     * Event handler. Called when a key was pressed.
     *
     * @param evt {qx.event.type.KeySequence} the event.
     */
    _onKeyPress : function(evt)
    {
      if (!this.getEnabled()) {
        return;
      }

      // No editing mode
      var oldFocusedRow = this.__focusedRow;
      var consumed = true;

      // Handle keys that are independent from the modifiers
      var identifier = evt.getKeyIdentifier();

      if (this.isEditing())
      {
        // Editing mode
        if (evt.getModifiers() == 0)
        {
          switch(identifier)
          {
            case "Enter":
              this.stopEditing();
              var oldFocusedRow = this.__focusedRow;
              this.moveFocusedCell(0, 1);

              if (this.__focusedRow != oldFocusedRow) {
                consumed = this.startEditing();
              }

              break;

            case "Escape":
              this.cancelEditing();
              this.focus();
              break;

            default:
              consumed = false;
              break;
          }
        }

      }
      else
      {
        // No editing mode
        if (evt.isCtrlPressed())
        {
          // Handle keys that depend on modifiers
          consumed = true;

          switch(identifier)
          {
            case "A": // Ctrl + A
              var rowCount = this.getTableModel().getRowCount();

              if (rowCount > 0) {
                this.getSelectionModel().setSelectionInterval(0, rowCount - 1);
              }

              break;

            default:
              consumed = false;
              break;
          }
        }
        else
        {
          // Handle keys that are independent from the modifiers
          switch(identifier)
          {
            case "Space":
              this.__selectionManager.handleSelectKeyDown(this.__focusedRow, evt);
              break;

            case "F2":
            case "Enter":
              this.startEditing();
              consumed = true;
              break;

            case "Home":
              this.setFocusedCell(this.__focusedCol, 0, true);
              break;

            case "End":
              var rowCount = this.getTableModel().getRowCount();
              this.setFocusedCell(this.__focusedCol, rowCount - 1, true);
              break;

            case "Left":
              this.moveFocusedCell(-1, 0);
              break;

            case "Right":
              this.moveFocusedCell(1, 0);
              break;

            case "Up":
              this.moveFocusedCell(0, -1);
              break;

            case "Down":
              this.moveFocusedCell(0, 1);
              break;

            case "PageUp":
            case "PageDown":
              var scroller = this.getPaneScroller(0);
              var pane = scroller.getTablePane();
              var rowHeight = this.getRowHeight();
              var direction = (identifier == "PageUp") ? -1 : 1;
              rowCount = pane.getVisibleRowCount() - 1;
              scroller.setScrollY(scroller.getScrollY() + direction * rowCount * rowHeight);
              this.moveFocusedCell(0, direction * rowCount);
              break;

            default:
              consumed = false;
          }
        }
      }

      if (oldFocusedRow != this.__focusedRow &&
          this.getRowFocusChangeModifiesSelection())
      {
        // The focus moved -> Let the selection manager handle this event
        this.__selectionManager.handleMoveKeyDown(this.__focusedRow, evt);
      }

      if (consumed)
      {
        evt.preventDefault();
        evt.stopPropagation();
      }
    },


    /**
     * Event handler. Called when the table gets the focus.
     *
     * @param evt {Map} the event.
     */
    _onFocusChanged : function(evt)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].onFocusChanged();
      }
    },


    /**
     * Event handler. Called when the visibility of a column has changed.
     *
     * @param evt {Map} the event.
     */
    _onColVisibilityChanged : function(evt)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].onColVisibilityChanged();
      }

      var data = evt.getData();
      if (this.__columnMenuButtons != null && data.col != null &&
          data.visible != null) {
        this.__columnMenuButtons[data.col].setVisible(data.visible);
      }

      this._updateScrollerWidths();
      this._updateScrollBarVisibility();
    },


    /**
     * Event handler. Called when the width of a column has changed.
     *
     * @param evt {Map} the event.
     */
    _onColWidthChanged : function(evt)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++)
      {
        var data = evt.getData();
        scrollerArr[i].setColumnWidth(data.col, data.newWidth);
      }

      this._updateScrollerWidths();
      this._updateScrollBarVisibility();
    },


    /**
     * Event handler. Called when the column order has changed.
     *
     * @param evt {Map} the event.
     */
    _onColOrderChanged : function(evt)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].onColOrderChanged();
      }

      // A column may have been moved between meta columns
      this._updateScrollerWidths();
      this._updateScrollBarVisibility();
    },


    /**
     * Gets the TablePaneScroller at a certain x position in the page. If there is
     * no TablePaneScroller at this position, null is returned.
     *
     * @param pageX {Integer} the position in the page to check (in pixels).
     * @return {qx.ui.table.pane.Scroller} the TablePaneScroller or null.
     */
    getTablePaneScrollerAtPageX : function(pageX)
    {
      var metaCol = this._getMetaColumnAtPageX(pageX);
      return (metaCol != -1) ? this.getPaneScroller(metaCol) : null;
    },


    /**
     * Sets the currently focused cell. A value of <code>null</code> hides the
     * focus cell.
     *
     * @param col {Integer?null} the model index of the focused cell's column.
     * @param row {Integer?null} the model index of the focused cell's row.
     * @param scrollVisible {Boolean ? false} whether to scroll the new focused cell
     *          visible.
     */
    setFocusedCell : function(col, row, scrollVisible)
    {
      if (!this.isEditing() && (col != this.__focusedCol || row != this.__focusedRow))
      {
        if (col === null) {
          col = 0;
        }

        this.__focusedCol = col;
        this.__focusedRow = row;

        var scrollerArr = this._getPaneScrollerArr();

        for (var i=0; i<scrollerArr.length; i++) {
          scrollerArr[i].setFocusedCell(col, row);
        }

        if (col != null && scrollVisible) {
          this.scrollCellVisible(col, row);
        }
      }
    },


    /**
     * Resets (clears) the current selection
     */
    resetSelection : function() {
      this.getSelectionModel().resetSelection();
    },


    /**
     * Resets the focused cell.
     */
    resetCellFocus : function() {
      this.setFocusedCell(null, null, false);
    },


    /**
     * Returns the column of the currently focused cell.
     *
     * @return {Integer} the model index of the focused cell's column.
     */
    getFocusedColumn : function() {
      return this.__focusedCol;
    },


    /**
     * Returns the row of the currently focused cell.
     *
     * @return {Integer} the model index of the focused cell's column.
     */
    getFocusedRow : function() {
      return this.__focusedRow;
    },


    /**
     * Select whether the focused row is highlighted
     *
     * @param bHighlight {Boolean}
     *   Flag indicating whether the focused row should be highlighted.
     *
     */
    highlightFocusedRow : function(bHighlight)
    {
      this.getDataRowRenderer().setHighlightFocusRow(bHighlight);
    },


    /**
     * Remove the highlighting of the current focus row.
     *
     * This is used to temporarily remove the highlighting of the currently
     * focused row, and is expected to be used most typically by adding a
     * listener on the "mouseout" event, so that the focus highlighting is
     * suspended when the mouse leaves the table:
     *
     *     table.addListener("mouseout", table.clearFocusedRowHighlight);
     *
     * @param evt {qx.event.type.Mouse} Incoming mouse event
     */
    clearFocusedRowHighlight : function(evt)
    {
      if(evt)
      {
        var relatedTarget = evt.getRelatedTarget();
        if (
          relatedTarget instanceof qx.ui.table.pane.Pane ||
          relatedTarget instanceof qx.ui.table.pane.FocusIndicator
         ) {
           return ;
         }
      }

      // Remove focus from any cell that has it
      this.resetCellFocus();

      // Now, for each pane scroller...
      var scrollerArr = this._getPaneScrollerArr();
      for (var i=0; i<scrollerArr.length; i++)
      {
        // ... repaint without focus.
        scrollerArr[i].onFocusChanged();
      }
    },


    /**
     * Moves the focus.
     *
     * @param deltaX {Integer} The delta by which the focus should be moved on the x axis.
     * @param deltaY {Integer} The delta by which the focus should be moved on the y axis.
     */
    moveFocusedCell : function(deltaX, deltaY)
    {
      var col = this.__focusedCol;
      var row = this.__focusedRow;

      // could also be undefined [BUG #4676]
      if (col == null || row == null) {
        return;
      }

      if (deltaX != 0)
      {
        var columnModel = this.getTableColumnModel();
        var x = columnModel.getVisibleX(col);
        var colCount = columnModel.getVisibleColumnCount();
        x = qx.lang.Number.limit(x + deltaX, 0, colCount - 1);
        col = columnModel.getVisibleColumnAtX(x);
      }

      if (deltaY != 0)
      {
        var tableModel = this.getTableModel();
        row = qx.lang.Number.limit(row + deltaY, 0, tableModel.getRowCount() - 1);
      }

      this.setFocusedCell(col, row, true);
    },


    /**
     * Scrolls a cell visible.
     *
     * @param col {Integer} the model index of the column the cell belongs to.
     * @param row {Integer} the model index of the row the cell belongs to.
     */
    scrollCellVisible : function(col, row)
    {
      // get the dom element
      var elem = this.getContentElement().getDomElement();
      // if the dom element is not available, the table hasn't been rendered
      if (!elem) {
        // postpone the scroll until the table has appeared
        this.addListenerOnce("appear", function() {
          this.scrollCellVisible(col, row);
        }, this);
      }

      var columnModel = this.getTableColumnModel();
      var x = columnModel.getVisibleX(col);

      var metaColumn = this._getMetaColumnAtColumnX(x);

      if (metaColumn != -1) {
        this.getPaneScroller(metaColumn).scrollCellVisible(col, row);
      }
    },


    /**
     * Returns whether currently a cell is editing.
     *
     * @return {var} whether currently a cell is editing.
     */
    isEditing : function()
    {
      if (this.__focusedCol != null)
      {
        var x = this.getTableColumnModel().getVisibleX(this.__focusedCol);
        var metaColumn = this._getMetaColumnAtColumnX(x);
        return this.getPaneScroller(metaColumn).isEditing();
      }
      return false;
    },


    /**
     * Starts editing the currently focused cell. Does nothing if already editing
     * or if the column is not editable.
     *
     * @return {Boolean} whether editing was started
     */
    startEditing : function()
    {
      if (this.__focusedCol != null)
      {
        var x = this.getTableColumnModel().getVisibleX(this.__focusedCol);
        var metaColumn = this._getMetaColumnAtColumnX(x);
        var started = this.getPaneScroller(metaColumn).startEditing();
        return started;
      }

      return false;
    },


    /**
     * Stops editing and writes the editor's value to the model.
     */
    stopEditing : function()
    {
      if (this.__focusedCol != null)
      {
        var x = this.getTableColumnModel().getVisibleX(this.__focusedCol);
        var metaColumn = this._getMetaColumnAtColumnX(x);
        this.getPaneScroller(metaColumn).stopEditing();
      }
    },


    /**
     * Stops editing without writing the editor's value to the model.
     */
    cancelEditing : function()
    {
      if (this.__focusedCol != null)
      {
        var x = this.getTableColumnModel().getVisibleX(this.__focusedCol);
        var metaColumn = this._getMetaColumnAtColumnX(x);
        this.getPaneScroller(metaColumn).cancelEditing();
      }
    },


    /**
     * Update the table content of every attached table pane.
     */
    updateContent : function() {
      var scrollerArr = this._getPaneScrollerArr();
      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].getTablePane().updateContent(true);
      }
    },

    /**
     * Activates the blocker widgets on all column headers and the
     * column button
     */
    blockHeaderElements : function()
    {
      var scrollerArr = this._getPaneScrollerArr();
      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].getHeader().getBlocker().blockContent(20);
      }
      this.getChildControl("column-button").getBlocker().blockContent(20);
    },


    /**
     * Deactivates the blocker widgets on all column headers and the
     * column button
     */
    unblockHeaderElements : function()
    {
      var scrollerArr = this._getPaneScrollerArr();
      for (var i=0; i<scrollerArr.length; i++) {
        scrollerArr[i].getHeader().getBlocker().unblock();
      }
      this.getChildControl("column-button").getBlocker().unblock();
    },

    /**
     * Gets the meta column at a certain x position in the page. If there is no
     * meta column at this position, -1 is returned.
     *
     * @param pageX {Integer} the position in the page to check (in pixels).
     * @return {Integer} the index of the meta column or -1.
     */
    _getMetaColumnAtPageX : function(pageX)
    {
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++)
      {
        var pos = scrollerArr[i].getContentLocation();

        if (pageX >= pos.left && pageX <= pos.right) {
          return i;
        }
      }

      return -1;
    },


    /**
     * Returns the meta column a column is shown in. If the column is not shown at
     * all, -1 is returned.
     *
     * @param visXPos {Integer} the visible x position of the column.
     * @return {Integer} the meta column the column is shown in.
     */
    _getMetaColumnAtColumnX : function(visXPos)
    {
      var metaColumnCounts = this.getMetaColumnCounts();
      var rightXPos = 0;

      for (var i=0; i<metaColumnCounts.length; i++)
      {
        var counts = metaColumnCounts[i];
        rightXPos += counts;

        if (counts == -1 || visXPos < rightXPos) {
          return i;
        }
      }

      return -1;
    },


    /**
     * Updates the text shown in the status bar.
     */
    _updateStatusBar : function()
    {
      var tableModel = this.getTableModel();

      if (this.getStatusBarVisible())
      {
        var selectedRowCount = this.getSelectionModel().getSelectedCount();
        var rowCount = tableModel.getRowCount();

        var text;

        if (rowCount >= 0)
        {
          if (selectedRowCount == 0) {
            text = this.trn("one row", "%1 rows", rowCount, rowCount);
          } else {
            text = this.trn("one of one row", "%1 of %2 rows", rowCount, selectedRowCount, rowCount);
          }
        }

        if (this.__additionalStatusBarText)
        {
          if (text) {
            text += this.__additionalStatusBarText;
          } else {
            text = this.__additionalStatusBarText;
          }
        }

        if (text) {
          this.getChildControl("statusbar").setValue(text);
        }
      }
    },


    /**
     * Updates the widths of all scrollers.
     */
    _updateScrollerWidths : function()
    {
      // Give all scrollers except for the last one the wanted width
      // (The last one has a flex with)
      var scrollerArr = this._getPaneScrollerArr();

      for (var i=0; i<scrollerArr.length; i++)
      {
        var isLast = (i == (scrollerArr.length - 1));
        var width = scrollerArr[i].getTablePaneModel().getTotalWidth();
        scrollerArr[i].setPaneWidth(width);

        var flex = isLast ? 1 : 0;
        scrollerArr[i].setLayoutProperties({flex: flex});
      }
    },


    /**
     * Updates the visibility of the scrollbars in the meta columns.
     */
    _updateScrollBarVisibility : function()
    {
      if (!this.getBounds()) {
        return;
      }

      var horBar = qx.ui.table.pane.Scroller.HORIZONTAL_SCROLLBAR;
      var verBar = qx.ui.table.pane.Scroller.VERTICAL_SCROLLBAR;
      var scrollerArr = this._getPaneScrollerArr();

      // Check which scroll bars are needed
      var horNeeded = false;
      var verNeeded = false;

      for (var i=0; i<scrollerArr.length; i++)
      {
        var isLast = (i == (scrollerArr.length - 1));

        // Only show the last vertical scrollbar
        var bars = scrollerArr[i].getNeededScrollBars(horNeeded, !isLast);

        if (bars & horBar) {
          horNeeded = true;
        }

        if (isLast && (bars & verBar)) {
          verNeeded = true;
        }
      }

      // Set the needed scrollbars
      for (var i=0; i<scrollerArr.length; i++)
      {
        var isLast = (i == (scrollerArr.length - 1));

        // Only show the last vertical scrollbar
        scrollerArr[i].setHorizontalScrollBarVisible(horNeeded);

        // If this is the last meta-column...
        if (isLast)
        {
          // ... then get the current (old) use of vertical scroll bar
          if (this.__hadVerticalScrollBar == null) {
            this.__hadVerticalScrollBar = scrollerArr[i].getVerticalScrollBarVisible();
            this.__timer = qx.event.Timer.once(function()
            {
              // reset the last visible state of the vertical scroll bar
              // in a timeout to prevent infinite loops.
              this.__hadVerticalScrollBar = null;
              this.__timer = null;
            }, this, 0);
          }
        }

        scrollerArr[i].setVerticalScrollBarVisible(isLast && verNeeded);

        // If this is the last meta-column and the use of a vertical scroll bar
        // has changed...
        if (isLast && verNeeded != this.__hadVerticalScrollBar)
        {
          // ... then dispatch an event to any awaiting listeners
          this.fireDataEvent("verticalScrollBarChanged", verNeeded);
        }
      }
    },


    /**
     * Initialize the column menu
     */
    _initColumnMenu : function()
    {
      var tableModel = this.getTableModel();
      var columnModel = this.getTableColumnModel();

      var columnButton = this.getChildControl("column-button");

      // Remove all items from the menu. We'll rebuild it here.
      columnButton.empty();

      // Inform listeners who may want to insert menu items at the beginning
      var menu = columnButton.getMenu();
      var data =
      {
        table        : this,
        menu         : menu,
        columnButton : columnButton
      };
      this.fireDataEvent("columnVisibilityMenuCreateStart", data);

      this.__columnMenuButtons = {};
      for (var col=0, l=tableModel.getColumnCount(); col<l; col++)
      {
        var menuButton =
          columnButton.factory("menu-button",
                               {
                                 text     : tableModel.getColumnName(col),
                                 column   : col,
                                 bVisible : columnModel.isColumnVisible(col)
                               });

        qx.core.Assert.assertInterface(menuButton,
                                       qx.ui.table.IColumnMenuItem);

        menuButton.addListener(
          "changeVisible",
          this._createColumnVisibilityCheckBoxHandler(col), this);
        this.__columnMenuButtons[col] = menuButton;
      }

      // Inform listeners who may want to insert menu items at the end
      data =
      {
        table        : this,
        menu         : menu,
        columnButton : columnButton
      };
      this.fireDataEvent("columnVisibilityMenuCreateEnd", data);
    },





    /**
     * Creates a handler for a check box of the column visibility menu.
     *
     * @param col {Integer} the model index of column to create the handler for.
     * @return {Function} The created event handler.
     */
    _createColumnVisibilityCheckBoxHandler : function(col)
    {
      return function(evt)
      {
        var columnModel = this.getTableColumnModel();
        columnModel.setColumnVisible(col, evt.getData());
      };
    },


    /**
     * Sets the width of a column.
     *
     * @param col {Integer} the model index of column.
     * @param width {Integer} the new width in pixels.
     */
    setColumnWidth : function(col, width) {
      this.getTableColumnModel().setColumnWidth(col, width);
    },


    /**
     * Resize event handler
     */
    _onResize : function()
    {
      this.fireEvent("tableWidthChanged");
      this._updateScrollerWidths();
      this._updateScrollBarVisibility();
    },


    // overridden
    addListener : function(type, listener, self, capture)
    {
      if (this.self(arguments).__redirectEvents[type])
      {
        // start the id with the type (needed for removing)
        var id = [type];
        for (var i = 0, arr = this._getPaneScrollerArr(); i < arr.length; i++)
        {
          id.push(arr[i].addListener.apply(arr[i], arguments));
        }
        // join the id's of every event with "
        return id.join('"');
      }
      else
      {
        return this.base(arguments, type, listener, self, capture);
      }
    },


    // overridden
    removeListener : function(type, listener, self, capture)
    {
      if (this.self(arguments).__redirectEvents[type])
      {
        for (var i = 0, arr = this._getPaneScrollerArr(); i < arr.length; i++)
        {
          arr[i].removeListener.apply(arr[i], arguments);
        }
      }
      else
      {
        this.base(arguments, type, listener, self, capture);
      }
    },


    // overridden
    removeListenerById : function(id) {
      var ids = id.split('"');
      // type is the first entry of the connected id
      var type = ids.shift();
      if (this.self(arguments).__redirectEvents[type])
      {
        var removed = true;
        for (var i = 0, arr = this._getPaneScrollerArr(); i < arr.length; i++)
        {
          removed = arr[i].removeListenerById.call(arr[i], ids[i]) && removed;
        }
        return removed;
      }
      else
      {
        return this.base(arguments, id);
      }
    },


    destroy : function()
    {
      this.getChildControl("column-button").getMenu().destroy();
      this.base(arguments);
    }
  },




  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    // remove the event listener which handled the locale change
    if (qx.core.Environment.get("qx.dynlocale")) {
      qx.locale.Manager.getInstance().removeListener("changeLocale", this._onChangeLocale, this);
    }

    // we allocated these objects on init so we have to clean them up.
    var selectionModel = this.getSelectionModel();
    if (selectionModel) {
      selectionModel.dispose();
    }

    var dataRowRenderer = this.getDataRowRenderer();
    if (dataRowRenderer) {
      dataRowRenderer.dispose();
    }

    this._cleanUpMetaColumns(0);
    this.getTableColumnModel().dispose();
    this._disposeObjects(
      "__selectionManager", "__scrollerParent",
      "__emptyTableModel", "__emptyTableModel",
      "__columnModel", "__timer"
    );
    this._disposeMap("__columnMenuButtons");
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)

************************************************************************ */

/**
 * Interface for a row renderer.
 */
qx.Interface.define("qx.ui.table.IRowRenderer",
{
  members :
  {
    /**
     * Updates a data row.
     *
     * The rowInfo map contains the following properties:
     * <ul>
     * <li>rowData (var): contains the row data for the row.
     *   The kind of this object depends on the table model, see
     *   {@link ITableModel#getRowData()}</li>
     * <li>row (int): the model index of the row.</li>
     * <li>selected (boolean): whether a cell in this row is selected.</li>
     * <li>focusedRow (boolean): whether the focused cell is in this row.</li>
     * <li>table (qx.ui.table.Table): the table the row belongs to.</li>
     * </ul>
     *
     * @abstract
     * @param rowInfo {Map} A map containing the information about the row to
     *      update.
     * @param rowElement {Element} the DOM element that renders the data row.
     */
    updateDataRowElement : function(rowInfo, rowElement) {},


    /**
     * Get the row's height CSS style taking the box model into account
     *
     * @param height {Integer} The row's (border-box) height in pixel
     */
    getRowHeightStyle : function(height) {},


    /**
     * Create a style string, which will be set as the style property of the row.
     *
     * @param rowInfo {Map} A map containing the information about the row to
     *      update. See {@link #updateDataRowElement} for more information.
     */
    createRowStyle : function(rowInfo) {},


    /**
     * Create a HTML class string, which will be set as the class property of the row.
     *
     * @param rowInfo {Map} A map containing the information about the row to
     *      update. See {@link #updateDataRowElement} for more information.
     */
    getRowClass : function(rowInfo) {}

  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de
     2007 Visionet GmbH, http://www.visionet.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132) STZ-IDA
     * Dietrich Streifert (level420) Visionet

************************************************************************ */

/**
 * The default data row renderer.
 */
qx.Class.define("qx.ui.table.rowrenderer.Default",
{
  extend : qx.core.Object,
  implement : qx.ui.table.IRowRenderer,




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    this.initThemeValues();

    // dynamic theme switch
    if (qx.core.Environment.get("qx.dyntheme")) {
      qx.theme.manager.Color.getInstance().addListener(
        "changeTheme", this.initThemeValues, this
      );
      qx.theme.manager.Font.getInstance().addListener(
        "changeTheme", this.initThemeValues, this
      );
    }
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /** Whether the focused row should be highlighted. */
    highlightFocusRow :
    {
      check : "Boolean",
      init : true
    }
  },



  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    _colors : null,
    __fontStyle : null,
    __fontStyleString : null,

    /**
     * Initializes the colors from the color theme.
     * @internal
     */
    initThemeValues : function() {
      this.__fontStyleString = "";
      this.__fontStyle = {};

      this._colors = {};

      // link to font theme
      this._renderFont(qx.theme.manager.Font.getInstance().resolve("default"));

      // link to color theme
      var colorMgr = qx.theme.manager.Color.getInstance();
      this._colors.bgcolFocusedSelected = colorMgr.resolve("table-row-background-focused-selected");
      this._colors.bgcolFocused = colorMgr.resolve("table-row-background-focused");
      this._colors.bgcolSelected = colorMgr.resolve("table-row-background-selected");
      this._colors.bgcolEven = colorMgr.resolve("table-row-background-even");
      this._colors.bgcolOdd = colorMgr.resolve("table-row-background-odd");
      this._colors.colSelected = colorMgr.resolve("table-row-selected");
      this._colors.colNormal = colorMgr.resolve("table-row");
      this._colors.horLine = colorMgr.resolve("table-row-line");
    },


    /**
     * the sum of the vertical insets. This is needed to compute the box model
     * independent size
     */
    _insetY : 1, // borderBottom

    /**
     * Render the new font and update the table pane content
     * to reflect the font change.
     *
     * @param font {qx.bom.Font} The font to use for the table row
     */
    _renderFont : function(font)
    {
      if (font)
      {
        this.__fontStyle = font.getStyles();
        this.__fontStyleString = qx.bom.element.Style.compile(this.__fontStyle);
        this.__fontStyleString = this.__fontStyleString.replace(/"/g, "'");
      }
      else
      {
        this.__fontStyleString = "";
        this.__fontStyle = qx.bom.Font.getDefaultStyles();
      }
    },


    // interface implementation
    updateDataRowElement : function(rowInfo, rowElem)
    {
      var fontStyle = this.__fontStyle;
      var style = rowElem.style;

      // set font styles
      qx.bom.element.Style.setStyles(rowElem, fontStyle);

      if (rowInfo.focusedRow && this.getHighlightFocusRow())
      {
        style.backgroundColor = rowInfo.selected ? this._colors.bgcolFocusedSelected : this._colors.bgcolFocused;
      }
      else
      {
        if (rowInfo.selected)
        {
          style.backgroundColor = this._colors.bgcolSelected;
        }
        else
        {
          style.backgroundColor = (rowInfo.row % 2 == 0) ? this._colors.bgcolEven : this._colors.bgcolOdd;
        }
      }

      style.color = rowInfo.selected ? this._colors.colSelected : this._colors.colNormal;
      style.borderBottom = "1px solid " + this._colors.horLine;
    },


    /**
     * Get the row's height CSS style taking the box model into account
     *
     * @param height {Integer} The row's (border-box) height in pixel
     * @return {String} CSS rule for the row height
     */
    getRowHeightStyle : function(height)
    {
      if (qx.core.Environment.get("css.boxmodel") == "content") {
        height -= this._insetY;
      }

      return "height:" + height + "px;";
    },


    // interface implementation
    createRowStyle : function(rowInfo)
    {
      var rowStyle = [];
      rowStyle.push(";");
      rowStyle.push(this.__fontStyleString);
      rowStyle.push("background-color:");

      if (rowInfo.focusedRow && this.getHighlightFocusRow())
      {
        rowStyle.push(rowInfo.selected ? this._colors.bgcolFocusedSelected : this._colors.bgcolFocused);
      }
      else
      {
        if (rowInfo.selected)
        {
          rowStyle.push(this._colors.bgcolSelected);
        }
        else
        {
          rowStyle.push((rowInfo.row % 2 == 0) ? this._colors.bgcolEven : this._colors.bgcolOdd);
        }
      }

      rowStyle.push(';color:');
      rowStyle.push(rowInfo.selected ? this._colors.colSelected : this._colors.colNormal);

      rowStyle.push(';border-bottom: 1px solid ', this._colors.horLine);

      return rowStyle.join("");
    },


    getRowClass : function(rowInfo) {
      return "";
    },

    /**
     * Add extra attributes to each row.
     *
     * @param rowInfo {Object}
     *   The following members are available in rowInfo:
     *   <dl>
     *     <dt>table {qx.ui.table.Table}</dt>
     *     <dd>The table object</dd>
     *
     *     <dt>styleHeight {Integer}</dt>
     *     <dd>The height of this (and every) row</dd>
     *
     *     <dt>row {Integer}</dt>
     *     <dd>The number of the row being added</dd>
     *
     *     <dt>selected {Boolean}</dt>
     *     <dd>Whether the row being added is currently selected</dd>
     *
     *     <dt>focusedRow {Boolean}</dt>
     *     <dd>Whether the row being added is currently focused</dd>
     *
     *     <dt>rowData {Array}</dt>
     *     <dd>The array row from the data model of the row being added</dd>
     *   </dl>
     *
     * @return {String}
     *   Any additional attributes and their values that should be added to the
     *   div tag for the row.
     */
    getRowAttributes : function(rowInfo)
    {
      return "";
    }
  },




  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function() {
    this._colors = this.__fontStyle = this.__fontStyleString = null;

    // remove dynamic theme listener
    if (qx.core.Environment.get("qx.dyntheme")) {
      qx.theme.manager.Color.getInstance().removeListener(
        "changeTheme", this.initThemeValues, this
      );
      qx.theme.manager.Font.getInstance().removeListener(
        "changeTheme", this.initThemeValues, this
      );
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2009 Derrell Lipman

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Derrell Lipman (derrell)

************************************************************************ */

/**
 * Interface for creating the column visibility menu
 */
qx.Interface.define("qx.ui.table.IColumnMenuButton",
{
  properties :
  {
    /**
     * The menu which is displayed when this button is pressed.
     */
    menu : { }
  },

  members :
  {
    /**
     * Instantiate a sub-widget.
     *
     * @param item {String}
     *   One of the following strings, indicating what type of
     *   column-menu-specific object to instantiate:
     *   <dl>
     *     <dt>menu</dt>
     *     <dd>
     *       Instantiate a menu which will appear when the column visibility
     *       button is pressed. No options are provided in this case.
     *     </dd>
     *     <dt>menu-button</dt>
     *     <dd>
     *       Instantiate a button to correspond to a column within the
     *       table. The options are a map containing <i>text</i>, the name of
     *       the column; <i>column</i>, the column number; and
     *       <i>bVisible</i>, a boolean indicating whether this column is
     *       currently visible. The instantiated return object must implement
     *       interface {@link qx.ui.table.IColumnMenuItem}
     *     </dd>
     *     <dt>user-button</dt>
     *     <dd>
     *       Instantiate a button for other than a column name. This is used,
     *       for example, to add the "Reset column widths" button when the
     *       Resize column model is requested. The options is a map containing
     *       <i>text</i>, the text to present in the button.
     *     </dd>
     *     <dt>separator</dt>
     *     <dd>
     *       Instantiate a separator object to added to the menu. This is
     *       used, for example, to separate the table column name list from
     *       the "Reset column widths" button when the Resize column model is
     *       requested. No options are provided in this case.
     *     </dd>
     *   </dl>
     *
     * @param options {Map}
     *   Options specific to the <i>item</i> being requested.
     *
     * @return {qx.ui.core.Widget}
     *   The instantiated object as specified by <i>item</i>.
     */
    factory : function(item, options)
    {
      return true;
    },

    /**
     * Empty the menu of all items, in preparation for building a new column
     * visibility menu.
     *
     */
    empty : function()
    {
      return true;
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2009 Derrell Lipman

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Derrell Lipman (derrell)
     * Jonathan Weiß (jonathan_rass)

************************************************************************ */

/**
 * The traditional qx.ui.menu.MenuButton to access the column visibility menu.
 */
qx.Class.define("qx.ui.table.columnmenu.Button",
{
  extend     : qx.ui.form.MenuButton,
  implement  : qx.ui.table.IColumnMenuButton,

  /**
   * Create a new instance of a column visibility menu button. This button
   * also contains the factory for creating each of the sub-widgets.
   */
  construct : function()
  {
    this.base(arguments);

    // add blocker
    this.__blocker = new qx.ui.core.Blocker(this);
  },

  members :
  {
    __columnMenuButtons : null,
    __blocker : null,

    // Documented in qx.ui.table.IColumnMenu
    factory : function(item, options)
    {
      switch(item)
      {
        case "menu":
          var menu = new qx.ui.menu.Menu();
          this.setMenu(menu);
          return menu;

        case "menu-button":
          var menuButton =
            new qx.ui.table.columnmenu.MenuItem(options.text);
          menuButton.setVisible(options.bVisible);
          this.getMenu().add(menuButton);
          return menuButton;

        case "user-button":
          var button = new qx.ui.menu.Button(options.text);
          button.set(
            {
              appearance: "table-column-reset-button"
            });
          return button;

        case "separator":
          return new qx.ui.menu.Separator();

        default:
          throw new Error("Unrecognized factory request: " + item);
      }
    },


    /**
     * Returns the blocker of the columnmenu button.
     *
     * @return {qx.ui.core.Blocker} the blocker.
     */
    getBlocker : function() {
      return this.__blocker;
    },

    // Documented in qx.ui.table.IColumnMenu
    empty : function()
    {
      var menu = this.getMenu();
      var entries = menu.getChildren();

      for (var i=0,l=entries.length; i<l; i++)
      {
        entries[0].destroy();
      }
    }
  },

  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct: function() {
    this.__blocker.dispose();
  }

});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2009 Derrell Lipman

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Derrell Lipman (derrell)

************************************************************************ */

/**
 * Interface for a column menu item corresponding to a table column.
 */
qx.Interface.define("qx.ui.table.IColumnMenuItem",
{
  properties :
  {
    /**
     * Whether the table column associated with this menu item is visible
     */
    visible : { }
  },

  events :
  {
    /**
     * Dispatched when a column changes visibility state. The event data is a
     * boolean indicating whether the table column associated with this menu
     * item is now visible.
     */
    changeVisible : "qx.event.type.Data"
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)
     * Martin Wittemann (martinwittemann)

************************************************************************ */

/**
 * Renders a special checkbox button inside a menu. The button behaves like
 * a normal {@link qx.ui.form.CheckBox} and shows a check icon when
 * checked; normally shows no icon when not checked (depends on the theme).
 */
qx.Class.define("qx.ui.menu.CheckBox",
{
  extend : qx.ui.menu.AbstractButton,
  implement : [qx.ui.form.IBooleanForm],



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param label {String} Initial label
   * @param menu {qx.ui.menu.Menu} Initial sub menu
   */
  construct : function(label, menu)
  {
    this.base(arguments);

    // Initialize with incoming arguments
    if (label != null) {
      // try to translate every time you create a checkbox [BUG #2699]
      if (label.translate) {
        this.setLabel(label.translate());
      } else {
        this.setLabel(label);
      }
    }

    if (menu != null) {
      this.setMenu(menu);
    }

    this.addListener("execute", this._onExecute, this);
  },



  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    // overridden
    appearance :
    {
      refine : true,
      init : "menu-checkbox"
    },

    /** Whether the button is checked */
    value :
    {
      check : "Boolean",
      init : false,
      apply : "_applyValue",
      event : "changeValue",
      nullable : true
    }
  },





  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    // overridden (from MExecutable to keet the icon out of the binding)
    /**
     * @lint ignoreReferenceField(_bindableProperties)
     */
    _bindableProperties :
    [
      "enabled",
      "label",
      "toolTipText",
      "value",
      "menu"
    ],

    // property apply
    _applyValue : function(value, old)
    {
      value ?
        this.addState("checked") :
        this.removeState("checked");
    },


    /**
     * Handler for the execute event.
     *
     * @param e {qx.event.type.Event} The execute event.
     */
    _onExecute : function(e) {
      this.toggleValue();
    },


    // overridden
    _onClick : function(e)
    {
      if (e.isLeftPressed()) {
        this.execute();
      } else {
        // don't close menus if the button has a context menu
        if (this.getContextMenu()) {
          return;
        }
      }
      qx.ui.menu.Manager.getInstance().hideAll();
    },


    // overridden
    _onKeyPress : function(e) {
      this.execute();
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2009 Derrell Lipman

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Derrell Lipman (derrell)

************************************************************************ */

/**
 * A menu item.
 */
qx.Class.define("qx.ui.table.columnmenu.MenuItem",
{
  extend     : qx.ui.menu.CheckBox,
  implement  : qx.ui.table.IColumnMenuItem,

  properties :
  {
    /**
     * Whether the table column associated with this menu item is visible.
     */
    visible :
    {
      check : "Boolean",
      init  : true,
      apply : "_applyVisible",
      event : "changeVisible"
    }
  },

  /**
   * Create a new instance of an item for insertion into the table column
   * visibility menu.
   *
   * @param text {String}
   *   Text for the menu item, most typically the name of the column in the
   *   table.
   */
  construct : function(text)
  {
    this.base(arguments, text);

    // Mirror native "value" property in our "visible" property
    this.addListener("changeValue",
                     function(e)
                     {
                       this.bInListener = true;
                       this.setVisible(e.getData());
                       this.bInListener = false;
                     });
  },

  members :
  {
    __bInListener : false,

    /**
     * Keep menu in sync with programmatic changes of visibility
     *
     * @param value {Boolean}
     *   New visibility value
     *
     * @param old {Boolean}
     *   Previous visibility value
     */
    _applyVisible : function(value, old)
    {
      // avoid recursion if called from listener on "changeValue" property
      if (! this.bInListener)
      {
        this.setValue(value);
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)

************************************************************************ */

/**
 * A selection manager. This is a helper class that handles all selection
 * related events and updates a SelectionModel.
 * <p>
 * Widgets that support selection should use this manager. This way the only
 * thing the widget has to do is mapping mouse or key events to indexes and
 * call the corresponding handler method.
 *
 * @see SelectionModel
 */
qx.Class.define("qx.ui.table.selection.Manager",
{
  extend : qx.core.Object,




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function() {
    this.base(arguments);
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /**
     * The selection model where to set the selection changes.
     */
    selectionModel :
    {
      check : "qx.ui.table.selection.Model"
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __lastMouseDownHandled : null,


    /**
     * Handles the mouse down event.
     *
     * @param index {Integer} the index the mouse is pointing at.
     * @param evt {Map} the mouse event.
     */
    handleMouseDown : function(index, evt)
    {
      if (evt.isLeftPressed())
      {
        var selectionModel = this.getSelectionModel();

        if (!selectionModel.isSelectedIndex(index))
        {
          // This index is not selected -> We react when the mouse is pressed (because of drag and drop)
          this._handleSelectEvent(index, evt);
          this.__lastMouseDownHandled = true;
        }
        else
        {
          // This index is already selected -> We react when the mouse is released (because of drag and drop)
          this.__lastMouseDownHandled = false;
        }
      }
      else if (evt.isRightPressed() && evt.getModifiers() == 0)
      {
        var selectionModel = this.getSelectionModel();

        if (!selectionModel.isSelectedIndex(index))
        {
          // This index is not selected -> Set the selection to this index
          selectionModel.setSelectionInterval(index, index);
        }
      }
    },


    /**
     * Handles the mouse up event.
     *
     * @param index {Integer} the index the mouse is pointing at.
     * @param evt {Map} the mouse event.
     */
    handleMouseUp : function(index, evt)
    {
      if (evt.isLeftPressed() && !this.__lastMouseDownHandled) {
        this._handleSelectEvent(index, evt);
      }
    },


    /**
     * Handles the mouse click event.
     *
     * @param index {Integer} the index the mouse is pointing at.
     * @param evt {Map} the mouse event.
     */
    handleClick : function(index, evt) {},


    /**
     * Handles the key down event that is used as replacement for mouse clicks
     * (Normally space).
     *
     * @param index {Integer} the index that is currently focused.
     * @param evt {Map} the key event.
     */
    handleSelectKeyDown : function(index, evt) {
      this._handleSelectEvent(index, evt);
    },


    /**
     * Handles a key down event that moved the focus (E.g. up, down, home, end, ...).
     *
     * @param index {Integer} the index that is currently focused.
     * @param evt {Map} the key event.
     */
    handleMoveKeyDown : function(index, evt)
    {
      var selectionModel = this.getSelectionModel();

      switch(evt.getModifiers())
      {
        case 0:
          selectionModel.setSelectionInterval(index, index);
          break;

        case qx.event.type.Dom.SHIFT_MASK:
          var anchor = selectionModel.getAnchorSelectionIndex();

          if (anchor == -1) {
            selectionModel.setSelectionInterval(index, index);
          } else {
            selectionModel.setSelectionInterval(anchor, index);
          }

          break;
      }
    },


    /**
     * Handles a select event.
     *
     * @param index {Integer} the index the event is pointing at.
     * @param evt {Map} the mouse event.
     */
    _handleSelectEvent : function(index, evt)
    {
      var selectionModel = this.getSelectionModel();

      var leadIndex = selectionModel.getLeadSelectionIndex();
      var anchorIndex = selectionModel.getAnchorSelectionIndex();

      if (evt.isShiftPressed())
      {
        if (index != leadIndex || selectionModel.isSelectionEmpty())
        {
          // The lead selection index was changed
          if (anchorIndex == -1) {
            anchorIndex = index;
          }

          if (evt.isCtrlOrCommandPressed()) {
            selectionModel.addSelectionInterval(anchorIndex, index);
          } else {
            selectionModel.setSelectionInterval(anchorIndex, index);
          }
        }
      }
      else if (evt.isCtrlOrCommandPressed())
      {
        if (selectionModel.isSelectedIndex(index)) {
          selectionModel.removeSelectionInterval(index, index);
        } else {
          selectionModel.addSelectionInterval(index, index);
        }
      }
      else
      {
        // setSelectionInterval checks to see if the change is really necessary
        selectionModel.setSelectionInterval(index, index);
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)
     * David Perez Carmona (david-perez)

************************************************************************ */

/**
 * A selection model.
 */
qx.Class.define("qx.ui.table.selection.Model",
{
  extend : qx.core.Object,




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    this.__selectedRangeArr = [];
    this.__anchorSelectionIndex = -1;
    this.__leadSelectionIndex = -1;
    this.hasBatchModeRefCount = 0;
    this.__hadChangeEventInBatchMode = false;
  },



  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events: {
    /** Fired when the selection has changed. */
    "changeSelection" : "qx.event.type.Event"
  },



  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

  statics :
  {

    /** @type {int} The selection mode "none". Nothing can ever be selected. */
    NO_SELECTION                : 1,

    /** @type {int} The selection mode "single". This mode only allows one selected item. */
    SINGLE_SELECTION            : 2,


    /**
     * @type {int} The selection mode "single interval". This mode only allows one
     * continuous interval of selected items.
     */
    SINGLE_INTERVAL_SELECTION   : 3,


    /**
     * @type {int} The selection mode "multiple interval". This mode only allows any
     * selection.
     */
    MULTIPLE_INTERVAL_SELECTION : 4,


    /**
     * @type {int} The selection mode "multiple interval". This mode only allows any
     * selection. The difference with the previous one, is that multiple
     * selection is eased. A click on an item, toggles its selection state.
     * On the other hand, MULTIPLE_INTERVAL_SELECTION does this behavior only
     * when Ctrl-clicking an item.
     */
    MULTIPLE_INTERVAL_SELECTION_TOGGLE : 5
  },



  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /**
     * Set the selection mode. Valid values are {@link #NO_SELECTION},
     * {@link #SINGLE_SELECTION}, {@link #SINGLE_INTERVAL_SELECTION},
     * {@link #MULTIPLE_INTERVAL_SELECTION} and
     * {@link #MULTIPLE_INTERVAL_SELECTION_TOGGLE}.
     */
    selectionMode :
    {
      init : 2, //SINGLE_SELECTION,
      check : [1,2,3,4,5],
      //[ NO_SELECTION, SINGLE_SELECTION, SINGLE_INTERVAL_SELECTION, MULTIPLE_INTERVAL_SELECTION, MULTIPLE_INTERVAL_SELECTION_TOGGLE ],
      apply : "_applySelectionMode"
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __hadChangeEventInBatchMode : null,
    __anchorSelectionIndex : null,
    __leadSelectionIndex : null,
    __selectedRangeArr : null,


    // selectionMode property modifier
    _applySelectionMode : function(selectionMode) {
      this.resetSelection();
    },


    /**
     *
     * Activates / Deactivates batch mode. In batch mode, no change events will be thrown but
     * will be collected instead. When batch mode is turned off again and any events have
     * been collected, one event is thrown to inform the listeners.
     *
     * This method supports nested calling, i. e. batch mode can be turned more than once.
     * In this case, batch mode will not end until it has been turned off once for each
     * turning on.
     *
     * @param batchMode {Boolean} true to activate batch mode, false to deactivate
     * @return {Boolean} true if batch mode is active, false otherwise
     * @throws {Error} if batch mode is turned off once more than it has been turned on
     */
    setBatchMode : function(batchMode)
    {
      if (batchMode) {
        this.hasBatchModeRefCount += 1;
      }
      else
      {
        if (this.hasBatchModeRefCount == 0) {
          throw new Error("Try to turn off batch mode althoug it was not turned on.");
        }

        this.hasBatchModeRefCount -= 1;

        if (this.__hadChangeEventInBatchMode)
        {
          this.__hadChangeEventInBatchMode = false;
          this._fireChangeSelection();
        }
      }

      return this.hasBatchMode();
    },


    /**
     *
     * Returns whether batch mode is active. See setter for a description of batch mode.
     *
     * @return {Boolean} true if batch mode is active, false otherwise
     */
    hasBatchMode : function() {
      return this.hasBatchModeRefCount > 0;
    },


    /**
     * Returns the first argument of the last call to {@link #setSelectionInterval()},
     * {@link #addSelectionInterval()} or {@link #removeSelectionInterval()}.
     *
     * @return {Integer} the anchor selection index.
     */
    getAnchorSelectionIndex : function() {
      return this.__anchorSelectionIndex;
    },


    /**
     * Sets the anchor selection index. Only use this function, if you want manipulate
     * the selection manually.
     *
     * @param index {Integer} the index to set.
     */
    _setAnchorSelectionIndex : function(index) {
      this.__anchorSelectionIndex = index;
    },


    /**
     * Returns the second argument of the last call to {@link #setSelectionInterval()},
     * {@link #addSelectionInterval()} or {@link #removeSelectionInterval()}.
     *
     * @return {Integer} the lead selection index.
     */
    getLeadSelectionIndex : function() {
      return this.__leadSelectionIndex;
    },


    /**
     * Sets the lead selection index. Only use this function, if you want manipulate
     * the selection manually.
     *
     * @param index {Integer} the index to set.
     */
    _setLeadSelectionIndex : function(index) {
      this.__leadSelectionIndex = index;
    },


    /**
     * Returns an array that holds all the selected ranges of the table. Each
     * entry is a map holding information about the "minIndex" and "maxIndex" of the
     * selection range.
     *
     * @return {Map[]} array with all the selected ranges.
     */
    _getSelectedRangeArr : function() {
      return this.__selectedRangeArr;
    },


    /**
     * Resets (clears) the selection.
     */
    resetSelection : function()
    {
      if (!this.isSelectionEmpty())
      {
        this._resetSelection();
        this._fireChangeSelection();
      }
    },


    /**
     * Returns whether the selection is empty.
     *
     * @return {Boolean} whether the selection is empty.
     */
    isSelectionEmpty : function() {
      return this.__selectedRangeArr.length == 0;
    },


    /**
     * Returns the number of selected items.
     *
     * @return {Integer} the number of selected items.
     */
    getSelectedCount : function()
    {
      var selectedCount = 0;

      for (var i=0; i<this.__selectedRangeArr.length; i++)
      {
        var range = this.__selectedRangeArr[i];
        selectedCount += range.maxIndex - range.minIndex + 1;
      }

      return selectedCount;
    },


    /**
     * Returns whether an index is selected.
     *
     * @param index {Integer} the index to check.
     * @return {Boolean} whether the index is selected.
     */
    isSelectedIndex : function(index)
    {
      for (var i=0; i<this.__selectedRangeArr.length; i++)
      {
        var range = this.__selectedRangeArr[i];

        if (index >= range.minIndex && index <= range.maxIndex) {
          return true;
        }
      }

      return false;
    },


    /**
     * Returns the selected ranges as an array. Each array element has a
     * <code>minIndex</code> and a <code>maxIndex</code> property.
     *
     * @return {Map[]} the selected ranges.
     */
    getSelectedRanges : function()
    {
      // clone the selection array and the individual elements - this prevents the
      // caller from messing with the internal model
      var retVal = [];

      for (var i=0; i<this.__selectedRangeArr.length; i++)
      {
        retVal.push(
        {
          minIndex : this.__selectedRangeArr[i].minIndex,
          maxIndex : this.__selectedRangeArr[i].maxIndex
        });
      }

      return retVal;
    },


    /**
     * Calls an iterator function for each selected index.
     *
     * Usage Example:
     * <pre class='javascript'>
     * var selectedRowData = [];
     * mySelectionModel.iterateSelection(function(index) {
     *   selectedRowData.push(myTableModel.getRowData(index));
     * });
     * </pre>
     *
     * @param iterator {Function} the function to call for each selected index.
     *          Gets the current index as parameter.
     * @param object {var ? null} the object to use when calling the handler.
     *          (this object will be available via "this" in the iterator)
     */
    iterateSelection : function(iterator, object)
    {
      for (var i=0; i<this.__selectedRangeArr.length; i++)
      {
        for (var j=this.__selectedRangeArr[i].minIndex; j<=this.__selectedRangeArr[i].maxIndex; j++) {
          iterator.call(object, j);
        }
      }
    },


    /**
     * Sets the selected interval. This will clear the former selection.
     *
     * @param fromIndex {Integer} the first index of the selection (including).
     * @param toIndex {Integer} the last index of the selection (including).
     */
    setSelectionInterval : function(fromIndex, toIndex)
    {
      var me = this.self(arguments);

      switch(this.getSelectionMode())
      {
        case me.NO_SELECTION:
          return;

        case me.SINGLE_SELECTION:
          // Ensure there is actually a change of selection
          if (this.isSelectedIndex(toIndex)) {
            return;
          }

          fromIndex = toIndex;
          break;

        case me.MULTIPLE_INTERVAL_SELECTION_TOGGLE:
          this.setBatchMode(true);
          try
          {
            for (var i = fromIndex; i <= toIndex; i++)
            {
              if (!this.isSelectedIndex(i))
              {
                this._addSelectionInterval(i, i);
              }
              else
              {
                this.removeSelectionInterval(i, i);
              }
            }
          }
          catch (e) {
            throw e;
          }
          finally {
            this.setBatchMode(false);
          }
          this._fireChangeSelection();
          return;
      }

      this._resetSelection();
      this._addSelectionInterval(fromIndex, toIndex);

      this._fireChangeSelection();
    },


    /**
     * Adds a selection interval to the current selection.
     *
     * @param fromIndex {Integer} the first index of the selection (including).
     * @param toIndex {Integer} the last index of the selection (including).
     */
    addSelectionInterval : function(fromIndex, toIndex)
    {
      var SelectionModel = qx.ui.table.selection.Model;

      switch(this.getSelectionMode())
      {
        case SelectionModel.NO_SELECTION:
          return;

        case SelectionModel.MULTIPLE_INTERVAL_SELECTION:
        case SelectionModel.MULTIPLE_INTERVAL_SELECTION_TOGGLE:
          this._addSelectionInterval(fromIndex, toIndex);
          this._fireChangeSelection();
          break;

        default:
          this.setSelectionInterval(fromIndex, toIndex);
          break;
      }
    },


    /**
     * Removes an interval from the current selection.
     *
     * @param fromIndex {Integer} the first index of the interval (including).
     * @param toIndex {Integer} the last index of the interval (including).
     */
    removeSelectionInterval : function(fromIndex, toIndex)
    {
      this.__anchorSelectionIndex = fromIndex;
      this.__leadSelectionIndex = toIndex;

      var minIndex = Math.min(fromIndex, toIndex);
      var maxIndex = Math.max(fromIndex, toIndex);

      // Crop the affected ranges
      for (var i=0; i<this.__selectedRangeArr.length; i++)
      {
        var range = this.__selectedRangeArr[i];

        if (range.minIndex > maxIndex)
        {
          // We are done
          break;
        }
        else if (range.maxIndex >= minIndex)
        {
          // This range is affected
          var minIsIn = (range.minIndex >= minIndex) && (range.minIndex <= maxIndex);
          var maxIsIn = (range.maxIndex >= minIndex) && (range.maxIndex <= maxIndex);

          if (minIsIn && maxIsIn)
          {
            // This range is removed completely
            this.__selectedRangeArr.splice(i, 1);

            // Check this index another time
            i--;
          }
          else if (minIsIn)
          {
            // The range is cropped from the left
            range.minIndex = maxIndex + 1;
          }
          else if (maxIsIn)
          {
            // The range is cropped from the right
            range.maxIndex = minIndex - 1;
          }
          else
          {
            // The range is split
            var newRange =
            {
              minIndex : maxIndex + 1,
              maxIndex : range.maxIndex
            };

            this.__selectedRangeArr.splice(i + 1, 0, newRange);

            range.maxIndex = minIndex - 1;

            // We are done
            break;
          }
        }
      }

      // this._dumpRanges();
      this._fireChangeSelection();
    },


    /**
     * Resets (clears) the selection, but doesn't inform the listeners.
     */
    _resetSelection : function()
    {
      this.__selectedRangeArr = [];
      this.__anchorSelectionIndex = -1;
      this.__leadSelectionIndex = -1;
    },


    /**
     * Adds a selection interval to the current selection, but doesn't inform
     * the listeners.
     *
     * @param fromIndex {Integer} the first index of the selection (including).
     * @param toIndex {Integer} the last index of the selection (including).
     */
    _addSelectionInterval : function(fromIndex, toIndex)
    {
      this.__anchorSelectionIndex = fromIndex;
      this.__leadSelectionIndex = toIndex;

      var minIndex = Math.min(fromIndex, toIndex);
      var maxIndex = Math.max(fromIndex, toIndex);

      // Find the index where the new range should be inserted
      var newRangeIndex = 0;

      for (;newRangeIndex<this.__selectedRangeArr.length; newRangeIndex++)
      {
        var range = this.__selectedRangeArr[newRangeIndex];

        if (range.minIndex > minIndex) {
          break;
        }
      }

      // Add the new range
      this.__selectedRangeArr.splice(newRangeIndex, 0,
      {
        minIndex : minIndex,
        maxIndex : maxIndex
      });

      // Merge overlapping ranges
      var lastRange = this.__selectedRangeArr[0];

      for (var i=1; i<this.__selectedRangeArr.length; i++)
      {
        var range = this.__selectedRangeArr[i];

        if (lastRange.maxIndex + 1 >= range.minIndex)
        {
          // The ranges are overlapping -> merge them
          lastRange.maxIndex = Math.max(lastRange.maxIndex, range.maxIndex);

          // Remove the current range
          this.__selectedRangeArr.splice(i, 1);

          // Check this index another time
          i--;
        }
        else
        {
          lastRange = range;
        }
      }
    },

    // this._dumpRanges();
    /**
     * Logs the current ranges for debug perposes.
     *
     */
    _dumpRanges : function()
    {
      var text = "Ranges:";

      for (var i=0; i<this.__selectedRangeArr.length; i++)
      {
        var range = this.__selectedRangeArr[i];
        text += " [" + range.minIndex + ".." + range.maxIndex + "]";
      }

      this.debug(text);
    },


    /**
     * Fires the "changeSelection" event to all registered listeners. If the selection model
     * currently is in batch mode, only one event will be thrown when batch mode is ended.
     *
     */
    _fireChangeSelection : function()
    {
      if (this.hasBatchMode())
      {
        // In batch mode, remember event but do not throw (yet)
        this.__hadChangeEventInBatchMode = true;
      }
      else
      {
        // If not in batch mode, throw event
        this.fireEvent("changeSelection");
      }
    }
  },




  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function() {
    this.__selectedRangeArr = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * The table pane that shows a certain section from a table. This class handles
 * the display of the data part of a table and is therefore the base for virtual
 * scrolling.
 */
qx.Class.define("qx.ui.table.pane.Pane",
{
  extend : qx.ui.core.Widget,




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param paneScroller {qx.ui.table.pane.Scroller} the TablePaneScroller the header belongs to.
   */
  construct : function(paneScroller)
  {
    this.base(arguments);

    this.__paneScroller = paneScroller;

    this.__lastColCount = 0;
    this.__lastRowCount = 0;

    this.__rowCache = [];
  },


  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */


  events :
  {
    /**
     * Whether the current view port of the pane has not loaded data.
     * The data object of the event indicates if the table pane has to reload
     * data or not. Can be used to give the user feedback of the loading state
     * of the rows.
     */
    "paneReloadsData" : "qx.event.type.Data",

    /**
     * Whenever the content of the table pane has been updated (rendered)
     * trigger a paneUpdated event. This allows the canvas cellrenderer to act
     * once the new cells have been integrated in the dom.
     */
    "paneUpdated" : "qx.event.type.Event"
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /** The index of the first row to show. */
    firstVisibleRow :
    {
      check : "Number",
      init : 0,
      apply : "_applyFirstVisibleRow"
    },


    /** The number of rows to show. */
    visibleRowCount :
    {
      check : "Number",
      init : 0,
      apply : "_applyVisibleRowCount"
    },


    /**
     * Maximum number of cached rows. If the value is <code>-1</code> the cache
     * size is unlimited
     */
    maxCacheLines :
    {
      check : "Number",
      init : 1000,
      apply : "_applyMaxCacheLines"
    },

    // overridden
    allowShrinkX :
    {
      refine : true,
      init : false
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __lastRowCount : null,
    __lastColCount : null,

    __paneScroller : null,
    __tableContainer : null,

    __focusedRow : null,
    __focusedCol : null,

    // sparse array to cache rendered rows
    __rowCache : null,
    __rowCacheCount : 0,


    // property modifier
    _applyFirstVisibleRow : function(value, old) {
      this.updateContent(false, value-old);
    },


    // property modifier
    _applyVisibleRowCount : function(value, old) {
      this.updateContent(true);
    },


    // overridden
    _getContentHint : function()
    {
      // the preferred height is 400 pixel. We don't use rowCount * rowHeight
      // because this is typically too large.
      return {
        width: this.getPaneScroller().getTablePaneModel().getTotalWidth(),
        height: 400
      }
    },


    /**
     * Returns the TablePaneScroller this pane belongs to.
     *
     * @return {qx.ui.table.pane.Scroller} the TablePaneScroller.
     */
    getPaneScroller : function() {
      return this.__paneScroller;
    },


    /**
     * Returns the table this pane belongs to.
     *
     * @return {qx.ui.table.Table} the table.
     */
    getTable : function() {
      return this.__paneScroller.getTable();
    },


    /**
     * Sets the currently focused cell.
     *
     * @param col {Integer?null} the model index of the focused cell's column.
     * @param row {Integer?null} the model index of the focused cell's row.
     * @param massUpdate {Boolean ? false} Whether other updates are planned as well.
     *          If true, no repaint will be done.
     */
    setFocusedCell : function(col, row, massUpdate)
    {
      if (col != this.__focusedCol || row != this.__focusedRow)
      {
        var oldRow = this.__focusedRow;
        this.__focusedCol = col;
        this.__focusedRow = row;

        // Update the focused row background
        if (row != oldRow && !massUpdate)
        {
          if (oldRow !== null) {
            this.updateContent(false, null, oldRow, true);
          }
          if (row !== null) {
            this.updateContent(false, null, row, true);
          }
        }
      }
    },


    /**
     * Event handler. Called when the selection has changed.
     */
    onSelectionChanged : function() {
      this.updateContent(false, null, null, true);
    },


    /**
     * Event handler. Called when the table gets or looses the focus.
     */
    onFocusChanged : function() {
      this.updateContent(false, null, null, true);
    },


    /**
     * Sets the column width.
     *
     * @param col {Integer} the column to change the width for.
     * @param width {Integer} the new width.
     */
    setColumnWidth : function(col, width) {
      this.updateContent(true);
    },


    /**
     * Event handler. Called the column order has changed.
     *
     */
    onColOrderChanged : function() {
      this.updateContent(true);
    },


    /**
     * Event handler. Called when the pane model has changed.
     */
    onPaneModelChanged : function() {
      this.updateContent(true);
    },


    /**
     * Event handler. Called when the table model data has changed.
     *
     * @param firstRow {Integer} The index of the first row that has changed.
     * @param lastRow {Integer} The index of the last row that has changed.
     * @param firstColumn {Integer} The model index of the first column that has changed.
     * @param lastColumn {Integer} The model index of the last column that has changed.
     */
    onTableModelDataChanged : function(firstRow, lastRow, firstColumn, lastColumn)
    {
      this.__rowCacheClear();

      var paneFirstRow = this.getFirstVisibleRow();
      var rowCount = this.getVisibleRowCount();

      if (lastRow == -1 || lastRow >= paneFirstRow && firstRow < paneFirstRow + rowCount)
      {
        // The change intersects this pane
        this.updateContent();
      }
    },


    /**
     * Event handler. Called when the table model meta data has changed.
     *
     */
    onTableModelMetaDataChanged : function() {
      this.updateContent(true);
    },


    // property apply method
    _applyMaxCacheLines : function(value, old)
    {
      if (this.__rowCacheCount >= value && value !== -1) {
        this.__rowCacheClear();
      }
    },


    /**
     * Clear the row cache
     */
    __rowCacheClear : function()
    {
      this.__rowCache = [];
      this.__rowCacheCount = 0;
    },


    /**
     * Get a line from the row cache.
     *
     * @param row {Integer} Row index to get
     * @param selected {Boolean} Whether the row is currently selected
     * @param focused {Boolean} Whether the row is currently focused
     * @return {String|null} The cached row or null if a row with the given
     *     index is not cached.
     */
    __rowCacheGet : function(row, selected, focused)
    {
      if (!selected && !focused && this.__rowCache[row]) {
        return this.__rowCache[row];
      } else {
        return null;
      }
    },


    /**
     * Add a line to the row cache.
     *
     * @param row {Integer} Row index to set
     * @param rowString {String} computed row string to cache
     * @param selected {Boolean} Whether the row is currently selected
     * @param focused {Boolean} Whether the row is currently focused
     */
    __rowCacheSet : function(row, rowString, selected, focused)
    {
      var maxCacheLines = this.getMaxCacheLines();
      if (
        !selected &&
        !focused &&
        !this.__rowCache[row] &&
        maxCacheLines > 0
      ) {
        this._applyMaxCacheLines(maxCacheLines);
        this.__rowCache[row] = rowString;
        this.__rowCacheCount += 1;
      }
    },


    /**
     * Updates the content of the pane.
     *
     * @param completeUpdate {Boolean ? false} if true a complete update is performed.
     *      On a complete update all cell widgets are recreated.
     * @param scrollOffset {Integer ? null} If set specifies how many rows to scroll.
     * @param onlyRow {Integer ? null} if set only the specified row will be updated.
     * @param onlySelectionOrFocusChanged {Boolean ? false} if true, cell values won't
     *          be updated. Only the row background will.
     */
    updateContent : function(completeUpdate, scrollOffset, onlyRow, onlySelectionOrFocusChanged)
    {
      if (completeUpdate) {
        this.__rowCacheClear();
      }

      //var start = new Date();

      if (scrollOffset && Math.abs(scrollOffset) <= Math.min(10, this.getVisibleRowCount()))
      {
        //this.debug("scroll", scrollOffset);
        this._scrollContent(scrollOffset);
      }
      else if (onlySelectionOrFocusChanged && !this.getTable().getAlwaysUpdateCells())
      {
        //this.debug("update row styles");
        this._updateRowStyles(onlyRow);
      }
      else
      {
        //this.debug("full update");
        this._updateAllRows();
      }

      //this.debug("render time: " + (new Date() - start) + "ms");
    },


    /**
     * If only focus or selection changes it is sufficient to only update the
     * row styles. This method updates the row styles of all visible rows or
     * of just one row.
     *
     * @param onlyRow {Integer|null ? null} If this parameter is set only the row
     *     with this index is updated.
     */
    _updateRowStyles : function(onlyRow)
    {
      var elem = this.getContentElement().getDomElement();

      if (!elem || !elem.firstChild) {
        this._updateAllRows();
        return;
      }

      var table = this.getTable();
      var selectionModel = table.getSelectionModel();
      var tableModel = table.getTableModel();
      var rowRenderer = table.getDataRowRenderer();
      var rowNodes = elem.firstChild.childNodes;
      var cellInfo = { table : table };

      // We don't want to execute the row loop below more than necessary. If
      // onlyRow is not null, we want to do the loop only for that row.
      // In that case, we start at (set the "row" variable to) that row, and
      // stop at (set the "end" variable to the offset of) the next row.
      var row = this.getFirstVisibleRow();
      var y = 0;

      // How many rows do we need to update?
      var end = rowNodes.length;

      if (onlyRow != null)
      {
        // How many rows are we skipping?
        var offset = onlyRow - row;
        if (offset >= 0 && offset < end)
        {
          row = onlyRow;
          y = offset;
          end = offset + 1;
        } else
        {
          return;
        }
      }

      for (; y<end; y++, row++)
      {
        cellInfo.row = row;
        cellInfo.selected = selectionModel.isSelectedIndex(row);
        cellInfo.focusedRow = (this.__focusedRow == row);
        cellInfo.rowData = tableModel.getRowData(row);

        rowRenderer.updateDataRowElement(cellInfo, rowNodes[y]);
      };
    },


    /**
     * Get the HTML table fragment for the given row range.
     *
     * @param firstRow {Integer} Index of the first row
     * @param rowCount {Integer} Number of rows
     * @return {String} The HTML table fragment for the given row range.
     */
    _getRowsHtml : function(firstRow, rowCount)
    {
      var table = this.getTable();

      var selectionModel = table.getSelectionModel();
      var tableModel = table.getTableModel();
      var columnModel = table.getTableColumnModel();
      var paneModel = this.getPaneScroller().getTablePaneModel();
      var rowRenderer = table.getDataRowRenderer();

      tableModel.prefetchRows(firstRow, firstRow + rowCount - 1);

      var rowHeight = table.getRowHeight();
      var colCount = paneModel.getColumnCount();
      var left = 0;
      var cols = [];

      // precompute column properties
      for (var x=0; x<colCount; x++)
      {
        var col = paneModel.getColumnAtX(x);
        var cellWidth = columnModel.getColumnWidth(col);
        cols.push({
          col: col,
          xPos: x,
          editable: tableModel.isColumnEditable(col),
          focusedCol: this.__focusedCol == col,
          styleLeft: left,
          styleWidth: cellWidth
        });

        left += cellWidth;
      }

      var rowsArr = [];
      var paneReloadsData = false;
      for (var row=firstRow; row < firstRow + rowCount; row++)
      {
        var selected = selectionModel.isSelectedIndex(row);
        var focusedRow = (this.__focusedRow == row);

        var cachedRow = this.__rowCacheGet(row, selected, focusedRow);
        if (cachedRow) {
          rowsArr.push(cachedRow);
          continue;
        }

        var rowHtml = [];

        var cellInfo = { table : table };
        cellInfo.styleHeight = rowHeight;

        cellInfo.row = row;
        cellInfo.selected = selected;
        cellInfo.focusedRow = focusedRow;
        cellInfo.rowData = tableModel.getRowData(row);

        if (!cellInfo.rowData) {
          paneReloadsData = true;
        }

        rowHtml.push('<div ');

        var rowAttributes = rowRenderer.getRowAttributes(cellInfo);
        if (rowAttributes) {
          rowHtml.push(rowAttributes);
        }

        var rowClass = rowRenderer.getRowClass(cellInfo);
        if (rowClass) {
          rowHtml.push('class="', rowClass, '" ');
        }

        var rowStyle = rowRenderer.createRowStyle(cellInfo);
        rowStyle += ";position:relative;" + rowRenderer.getRowHeightStyle(rowHeight)+ "width:100%;";
        if (rowStyle) {
          rowHtml.push('style="', rowStyle, '" ');
        }
        rowHtml.push('>');

        var stopLoop = false;
        for (x=0; x<colCount && !stopLoop; x++)
        {
          var col_def = cols[x];
          for (var attr in col_def) {
            cellInfo[attr] = col_def[attr];
          }
          var col = cellInfo.col;

          // Use the "getValue" method of the tableModel to get the cell's
          // value working directly on the "rowData" object
          // (-> cellInfo.rowData[col];) is not a solution because you can't
          // work with the columnIndex -> you have to use the columnId of the
          // columnIndex This is exactly what the method "getValue" does
          cellInfo.value = tableModel.getValue(col, row);
          var cellRenderer = columnModel.getDataCellRenderer(col);

          // Retrieve the current default cell style for this column.
          cellInfo.style = cellRenderer.getDefaultCellStyle();

          // Allow a cell renderer to tell us not to draw any further cells in
          // the row. Older, or traditional cell renderers don't return a
          // value, however, from createDataCellHtml, so assume those are
          // returning false.
          //
          // Tested with http://tinyurl.com/333hyhv
          stopLoop =
            cellRenderer.createDataCellHtml(cellInfo, rowHtml) || false;
        }
        rowHtml.push('</div>');

        var rowString = rowHtml.join("");

        this.__rowCacheSet(row, rowString, selected, focusedRow);
        rowsArr.push(rowString);
      }
      this.fireDataEvent("paneReloadsData", paneReloadsData);
      return rowsArr.join("");
    },


    /**
     * Scrolls the pane's contents by the given offset.
     *
     * @param rowOffset {Integer} Number of lines to scroll. Scrolling up is
     *     represented by a negative offset.
     */
    _scrollContent : function(rowOffset)
    {
      var el = this.getContentElement().getDomElement();
      if (!(el && el.firstChild)) {
        this._updateAllRows();
        return;
      }

      var tableBody = el.firstChild;
      var tableChildNodes = tableBody.childNodes;
      var rowCount = this.getVisibleRowCount();
      var firstRow = this.getFirstVisibleRow();

      var tabelModel = this.getTable().getTableModel();
      var modelRowCount = 0;

      modelRowCount = tabelModel.getRowCount();

      // don't handle this special case here
      if (firstRow + rowCount > modelRowCount) {
        this._updateAllRows();
        return;
      }

      // remove old lines
      var removeRowBase = rowOffset < 0 ? rowCount + rowOffset : 0;
      var addRowBase = rowOffset < 0 ? 0: rowCount - rowOffset;

      for (var i=Math.abs(rowOffset)-1; i>=0; i--)
      {
        var rowElem = tableChildNodes[removeRowBase];
        try {
          tableBody.removeChild(rowElem);
        } catch(exp) {
          break;
        }
      }

      // render new lines
      if (!this.__tableContainer) {
        this.__tableContainer = document.createElement("div");
      }
      var tableDummy = '<div>';
      tableDummy += this._getRowsHtml(firstRow + addRowBase, Math.abs(rowOffset));
      tableDummy += '</div>';
      this.__tableContainer.innerHTML = tableDummy;
      var newTableRows = this.__tableContainer.firstChild.childNodes;

      // append new lines
      if (rowOffset > 0)
      {
        for (var i=newTableRows.length-1; i>=0; i--)
        {
          var rowElem = newTableRows[0];
          tableBody.appendChild(rowElem);
        }
      }
      else
      {
        for (var i=newTableRows.length-1; i>=0; i--)
        {
          var rowElem = newTableRows[newTableRows.length-1];
          tableBody.insertBefore(rowElem, tableBody.firstChild);
        }
      }

      // update focus indicator
      if (this.__focusedRow !== null)
      {
        this._updateRowStyles(this.__focusedRow - rowOffset);
        this._updateRowStyles(this.__focusedRow);
      }
      this.fireEvent("paneUpdated");
    },


    /**
     * Updates the content of the pane (implemented using array joins).
     */
    _updateAllRows : function()
    {
      var elem = this.getContentElement().getDomElement();
      if (!elem) {
        // pane has not yet been rendered
        this.addListenerOnce("appear", arguments.callee, this);
        return;
      }

      var table = this.getTable();

      var tableModel = table.getTableModel();
      var paneModel = this.getPaneScroller().getTablePaneModel();

      var colCount = paneModel.getColumnCount();
      var rowHeight = table.getRowHeight();
      var firstRow = this.getFirstVisibleRow();

      var rowCount = this.getVisibleRowCount();
      var modelRowCount = tableModel.getRowCount();

      if (firstRow + rowCount > modelRowCount) {
        rowCount = Math.max(0, modelRowCount - firstRow);
      }

      var rowWidth = paneModel.getTotalWidth();
      var htmlArr;

      // If there are any rows...
      if (rowCount > 0)
      {
        // ... then create a div for them and add the rows to it.
        htmlArr =
          [
            "<div style='",
            "width: 100%;",
            (table.getForceLineHeight()
             ? "line-height: " + rowHeight + "px;"
             : ""),
            "overflow: hidden;",
            "'>",
            this._getRowsHtml(firstRow, rowCount),
            "</div>"
          ];
      }
      else
      {
        // Otherwise, don't create the div, as even an empty div creates a
        // white row in IE.
        htmlArr = [];
      }

      var data = htmlArr.join("");
      elem.innerHTML = data;
      this.setWidth(rowWidth);

      this.__lastColCount = colCount;
      this.__lastRowCount = rowCount;
      this.fireEvent("paneUpdated");
    }

  },




  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function() {
    this.__tableContainer = this.__paneScroller = this.__rowCache = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)

************************************************************************ */

/**
 * Shows the header of a table.
 */
qx.Class.define("qx.ui.table.pane.Header",
{
  extend : qx.ui.core.Widget,




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param paneScroller {qx.ui.table.pane.Scroller} the TablePaneScroller the header belongs to.
   */
  construct : function(paneScroller)
  {
    this.base(arguments);
    this._setLayout(new qx.ui.layout.HBox());

    // add blocker
    this.__blocker = new qx.ui.core.Blocker(this);

    this.__paneScroller = paneScroller;
  },






  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __paneScroller : null,
    __moveFeedback : null,
    __lastMouseOverColumn : null,
    __blocker : null,

    /**
     * Returns the TablePaneScroller this header belongs to.
     *
     * @return {qx.ui.table.pane.Scroller} the TablePaneScroller.
     */
    getPaneScroller : function() {
      return this.__paneScroller;
    },


    /**
     * Returns the table this header belongs to.
     *
     * @return {qx.ui.table.Table} the table.
     */
    getTable : function() {
      return this.__paneScroller.getTable();
    },

    /**
     * Returns the blocker of the header.
     *
     * @return {qx.ui.core.Blocker} the blocker.
     */
    getBlocker : function() {
      return this.__blocker;
    },

    /**
     * Event handler. Called the column order has changed.
     *
     */
    onColOrderChanged : function() {
      this._updateContent(true);
    },


    /**
     * Event handler. Called when the pane model has changed.
     */
    onPaneModelChanged : function() {
      this._updateContent(true);
    },


    /**
     * Event handler. Called when the table model meta data has changed.
     *
     */
    onTableModelMetaDataChanged : function() {
      this._updateContent();
    },


    /**
     * Sets the column width. This overrides the width from the column model.
     *
     * @param col {Integer}
     *   The column to change the width for.
     *
     * @param width {Integer}
     *   The new width.
     *
     * @param isMouseAction {Boolean}
     *   <i>true</i> if the column width is being changed as a result of a
     *   mouse drag in the header; false or undefined otherwise.
     *
     */
    setColumnWidth : function(col, width, isMouseAction)
    {
      var child = this.getHeaderWidgetAtColumn(col);

      if (child != null) {
        child.setWidth(width);
      }
    },


    /**
     * Sets the column the mouse is currently over.
     *
     * @param col {Integer} the model index of the column the mouse is currently over or
     *      null if the mouse is over no column.
     */
    setMouseOverColumn : function(col)
    {
      if (col != this.__lastMouseOverColumn)
      {
        if (this.__lastMouseOverColumn != null)
        {
          var widget = this.getHeaderWidgetAtColumn(this.__lastMouseOverColumn);

          if (widget != null) {
            widget.removeState("hovered");
          }
        }

        if (col != null) {
          this.getHeaderWidgetAtColumn(col).addState("hovered");
        }

        this.__lastMouseOverColumn = col;
      }
    },


    /**
     * Get the header widget for the given column
     *
     * @param col {Integer} The column number
     * @return {qx.ui.table.headerrenderer.HeaderCell} The header cell widget
     */
    getHeaderWidgetAtColumn : function(col)
    {
      var xPos = this.getPaneScroller().getTablePaneModel().getX(col);
      return this._getChildren()[xPos];
    },


    /**
     * Shows the feedback shown while a column is moved by the user.
     *
     * @param col {Integer} the model index of the column to show the move feedback for.
     * @param x {Integer} the x position the left side of the feeback should have
     *      (in pixels, relative to the left side of the header).
     */
    showColumnMoveFeedback : function(col, x)
    {
      var pos = this.getContentLocation();

      if (this.__moveFeedback == null)
      {
        var table = this.getTable();
        var xPos = this.getPaneScroller().getTablePaneModel().getX(col);
        var cellWidget = this._getChildren()[xPos];

        var tableModel = table.getTableModel();
        var columnModel = table.getTableColumnModel();

        var cellInfo =
        {
          xPos  : xPos,
          col   : col,
          name  : tableModel.getColumnName(col),
          table : table
        };

        var cellRenderer = columnModel.getHeaderCellRenderer(col);
        var feedback = cellRenderer.createHeaderCell(cellInfo);

        var size = cellWidget.getBounds();

        // Configure the feedback
        feedback.setWidth(size.width);
        feedback.setHeight(size.height);
        feedback.setZIndex(1000000);
        feedback.setOpacity(0.8);
        feedback.setLayoutProperties({top: pos.top});

        this.getApplicationRoot().add(feedback);
        this.__moveFeedback = feedback;
      }

      this.__moveFeedback.setLayoutProperties({left: pos.left + x});
      this.__moveFeedback.show();
    },


    /**
     * Hides the feedback shown while a column is moved by the user.
     */
    hideColumnMoveFeedback : function()
    {
      if (this.__moveFeedback != null)
      {
        this.__moveFeedback.destroy();
        this.__moveFeedback = null;
      }
    },


    /**
     * Returns whether the column move feedback is currently shown.
     *
     * @return {Boolean} <code>true</code> whether the column move feedback is
     *    currently shown, <code>false</code> otherwise.
     */
    isShowingColumnMoveFeedback : function() {
      return this.__moveFeedback != null;
    },


    /**
     * Updates the content of the header.
     *
     * @param completeUpdate {Boolean} if true a complete update is performed. On a
     *      complete update all header widgets are recreated.
     */
    _updateContent : function(completeUpdate)
    {
      var table = this.getTable();
      var tableModel = table.getTableModel();
      var columnModel = table.getTableColumnModel();
      var paneModel = this.getPaneScroller().getTablePaneModel();

      var children = this._getChildren();
      var colCount = paneModel.getColumnCount();

      var sortedColumn = tableModel.getSortColumnIndex();

      // Remove all widgets on the complete update
      if (completeUpdate) {
        this._cleanUpCells();
      }

      // Update the header
      var cellInfo = {};
      cellInfo.sortedAscending = tableModel.isSortAscending();

      for (var x=0; x<colCount; x++)
      {
        var col = paneModel.getColumnAtX(x);
        if (col === undefined) {
          continue;
        }

        var colWidth = columnModel.getColumnWidth(col);

        var cellRenderer = columnModel.getHeaderCellRenderer(col);

        cellInfo.xPos = x;
        cellInfo.col = col;
        cellInfo.name = tableModel.getColumnName(col);
        cellInfo.editable = tableModel.isColumnEditable(col);
        cellInfo.sorted = (col == sortedColumn);
        cellInfo.table = table;

        // Get the cached widget
        var cachedWidget = children[x];

        // Create or update the widget
        if (cachedWidget == null)
        {
          // We have no cached widget -> create it
          cachedWidget = cellRenderer.createHeaderCell(cellInfo);

          cachedWidget.set(
          {
            width  : colWidth
          });

          this._add(cachedWidget);
        }
        else
        {
          // This widget already created before -> recycle it
          cellRenderer.updateHeaderCell(cellInfo, cachedWidget);
        }

        // set the states
        if (x === 0) {
          cachedWidget.addState("first");
          cachedWidget.removeState("last");
        } else if (x === colCount - 1) {
          cachedWidget.removeState("first");
          cachedWidget.addState("last");
        } else {
          cachedWidget.removeState("first");
          cachedWidget.removeState("last");
        }
      }
    },


    /**
     * Cleans up all header cells.
     *
     */
    _cleanUpCells : function()
    {
      var children = this._getChildren();

      for (var x=children.length-1; x>=0; x--)
      {
        var cellWidget = children[x];
        cellWidget.destroy();
      }
    }
  },



  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    this.__blocker.dispose();
    this._disposeObjects("__paneScroller");
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)
     * Jonathan Weiß (jonathan_rass)

************************************************************************ */

/**
 * Shows a whole meta column. This includes a {@link Header},
 * a {@link Pane} and the needed scroll bars. This class handles the
 * virtual scrolling and does all the mouse event handling.
 *
 * @childControl header {qx.ui.table.pane.Header} header pane
 * @childControl pane {qx.ui.table.pane.Pane} table pane to show the data
 * @childControl focus-indicator {qx.ui.table.pane.FocusIndicator} shows the current focused cell
 * @childControl resize-line {qx.ui.core.Widget} resize line widget
 * @childControl scrollbar-x {qx.ui.core.scroll.ScrollBar?qx.ui.core.scroll.NativeScrollBar}
 *               horizontal scrollbar widget (depends on the "qx.nativeScrollBars" setting which implementation is used)
 * @childControl scrollbar-y {qx.ui.core.scroll.ScrollBar?qx.ui.core.scroll.NativeScrollBar}
 *               vertical scrollbar widget (depends on the "qx.nativeScrollBars" setting which implementation is used)
 */
qx.Class.define("qx.ui.table.pane.Scroller",
{
  extend : qx.ui.core.Widget,
  include : [qx.ui.core.scroll.MScrollBarFactory],



  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @param table {qx.ui.table.Table} the table the scroller belongs to.
   */
  construct : function(table)
  {
    this.base(arguments);

    this.__table = table;

    // init layout
    var grid = new qx.ui.layout.Grid();
    grid.setColumnFlex(0, 1);
    grid.setRowFlex(1, 1);
    this._setLayout(grid);

    // init child controls
    this.__header = this._showChildControl("header");
    this.__tablePane = this._showChildControl("pane");

    // the top line containing the header clipper and the top right widget
    this.__top = new qx.ui.container.Composite(new qx.ui.layout.HBox()).set({
      minWidth: 0
    });
    this._add(this.__top, {row: 0, column: 0, colSpan: 2});

    // embed header into a scrollable container
    this._headerClipper = new qx.ui.table.pane.Clipper();
    this._headerClipper.add(this.__header);
    this._headerClipper.addListener("losecapture", this._onChangeCaptureHeader, this);
    this._headerClipper.addListener("mousemove", this._onMousemoveHeader, this);
    this._headerClipper.addListener("mousedown", this._onMousedownHeader, this);
    this._headerClipper.addListener("mouseup", this._onMouseupHeader, this);
    this._headerClipper.addListener("click", this._onClickHeader, this);
    this.__top.add(this._headerClipper, {flex: 1});

    // embed pane into a scrollable container
    this.__paneClipper = new qx.ui.table.pane.Clipper();
    this.__paneClipper.add(this.__tablePane);
    this.__paneClipper.addListener("mousewheel", this._onMousewheel, this);
    this.__paneClipper.addListener("mousemove", this._onMousemovePane, this);
    this.__paneClipper.addListener("mousedown", this._onMousedownPane, this);
    this.__paneClipper.addListener("mouseup", this._onMouseupPane, this);
    this.__paneClipper.addListener("click", this._onClickPane, this);
    this.__paneClipper.addListener("contextmenu", this._onContextMenu, this);
    this.__paneClipper.addListener("dblclick", this._onDblclickPane, this);
    this.__paneClipper.addListener("resize", this._onResizePane, this);

    // if we have overlayed scroll bars, we should use a separate container
    if (qx.core.Environment.get("os.scrollBarOverlayed")) {
      this.__clipperContainer = new qx.ui.container.Composite();
      this.__clipperContainer.setLayout(new qx.ui.layout.Canvas());
      this.__clipperContainer.add(this.__paneClipper, {edge: 0});
      this._add(this.__clipperContainer, {row: 1, column: 0});
    } else {
      this._add(this.__paneClipper, {row: 1, column: 0});
    }

    // init scroll bars
    this.__horScrollBar = this._showChildControl("scrollbar-x");
    this.__verScrollBar = this._showChildControl("scrollbar-y");

    // init focus indicator
    this.__focusIndicator = this.getChildControl("focus-indicator");
    // need to run the apply method at least once [BUG #4057]
    this.initShowCellFocusIndicator();

    // force creation of the resize line
    this.getChildControl("resize-line").hide();

    this.addListener("mouseout", this._onMouseout, this);
    this.addListener("appear", this._onAppear, this);
    this.addListener("disappear", this._onDisappear, this);

    this.__timer = new qx.event.Timer();
    this.__timer.addListener("interval", this._oninterval, this);
    this.initScrollTimeout();

  },




  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

  statics :
  {

    /** @type {int} The minimum width a column could get in pixels. */
    MIN_COLUMN_WIDTH         : 10,

    /** @type {int} The radius of the resize region in pixels. */
    RESIZE_REGION_RADIUS     : 5,


    /**
     * (int) The number of pixels the mouse may move between mouse down and mouse up
     * in order to count as a click.
     */
    CLICK_TOLERANCE          : 5,


    /**
     * (int) The mask for the horizontal scroll bar.
     * May be combined with {@link #VERTICAL_SCROLLBAR}.
     *
     * @see #getNeededScrollBars
     */
    HORIZONTAL_SCROLLBAR     : 1,


    /**
     * (int) The mask for the vertical scroll bar.
     * May be combined with {@link #HORIZONTAL_SCROLLBAR}.
     *
     * @see #getNeededScrollBars
     */
    VERTICAL_SCROLLBAR       : 2
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  events :
  {
    /** Dispatched if the pane is scolled horizontally */
    "changeScrollY" : "qx.event.type.Data",

    /** Dispatched if the pane is scrolled vertically */
    "changeScrollX" : "qx.event.type.Data",

    /**See {@link qx.ui.table.Table#cellClick}.*/
    "cellClick" : "qx.ui.table.pane.CellEvent",

    /*** See {@link qx.ui.table.Table#cellDblclick}.*/
    "cellDblclick" : "qx.ui.table.pane.CellEvent",

    /**See {@link qx.ui.table.Table#cellContextmenu}.*/
    "cellContextmenu" : "qx.ui.table.pane.CellEvent",

    /** Dispatched when a sortable header was clicked */
    "beforeSort" : "qx.event.type.Data"
  },





  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {

    /** Whether to show the horizontal scroll bar */
    horizontalScrollBarVisible :
    {
      check : "Boolean",
      init : false,
      apply : "_applyHorizontalScrollBarVisible",
      event : "changeHorizontalScrollBarVisible"
    },

    /** Whether to show the vertical scroll bar */
    verticalScrollBarVisible :
    {
      check : "Boolean",
      init : false,
      apply : "_applyVerticalScrollBarVisible",
      event : "changeVerticalScrollBarVisible"
    },

    /** The table pane model. */
    tablePaneModel :
    {
      check : "qx.ui.table.pane.Model",
      apply : "_applyTablePaneModel",
      event : "changeTablePaneModel"
    },


    /**
     * Whether column resize should be live. If false, during resize only a line is
     * shown and the real resize happens when the user releases the mouse button.
     */
    liveResize :
    {
      check : "Boolean",
      init : false
    },


    /**
     * Whether the focus should moved when the mouse is moved over a cell. If false
     * the focus is only moved on mouse clicks.
     */
    focusCellOnMouseMove :
    {
      check : "Boolean",
      init : false
    },


    /**
     * Whether to handle selections via the selection manager before setting the
     * focus.  The traditional behavior is to handle selections after setting the
     * focus, but setting the focus means redrawing portions of the table, and
     * some subclasses may want to modify the data to be displayed based on the
     * selection.
     */
    selectBeforeFocus :
    {
      check : "Boolean",
      init : false
    },


    /**
     * Whether the cell focus indicator should be shown
     */
    showCellFocusIndicator :
    {
      check : "Boolean",
      init : true,
      apply : "_applyShowCellFocusIndicator"
    },


    /**
     * By default, the "cellContextmenu" event is fired only when a data cell
     * is right-clicked. It is not fired when a right-click occurs in the
     * empty area of the table below the last data row. By turning on this
     * property, "cellContextMenu" events will also be generated when a
     * right-click occurs in that empty area. In such a case, row identifier
     * in the event data will be null, so event handlers can check (row ===
     * null) to handle this case.
     */
    contextMenuFromDataCellsOnly :
    {
      check : "Boolean",
      init : true
    },


    /**
     * Whether to reset the selection when a header cell is clicked. Since
     * most data models do not have provisions to retain a selection after
     * sorting, the default is to reset the selection in this case. Some data
     * models, however, do have the capability to retain the selection, so
     * when using those, this property should be set to false.
     */
    resetSelectionOnHeaderClick :
    {
      check : "Boolean",
      init : true
    },


    /**
     * Interval time (in milliseconds) for the table update timer.
     * Setting this to 0 clears the timer.
     */
    scrollTimeout :
    {
      check : "Integer",
      init : 100,
      apply : "_applyScrollTimeout"
    },


    appearance :
    {
      refine : true,
      init : "table-scroller"
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __lastRowCount : null,
    __table : null,

    __updateInterval : null,
    __updateContentPlanned : null,
    __onintervalWrapper : null,

    _moveColumn : null,
    __lastMoveColPos : null,
    _lastMoveTargetX : null,
    _lastMoveTargetScroller : null,
    __lastMoveMousePageX : null,

    __resizeColumn : null,
    __lastResizeMousePageX : null,
    __lastResizeWidth : null,

    __lastMouseDownCell : null,
    __firedClickEvent : false,
    __ignoreClick : null,
    __lastMousePageX : null,
    __lastMousePageY : null,

    __focusedCol : null,
    __focusedRow : null,

    __cellEditor : null,
    __cellEditorFactory : null,

    __topRightWidget : null,
    __horScrollBar : null,
    __verScrollBar : null,
    __header : null,
    _headerClipper : null,
    __tablePane : null,
    __paneClipper : null,
    __clipperContainer : null,
    __focusIndicator : null,
    __top : null,

    __timer : null,


    /**
     * The right inset of the pane. The right inset is the maximum of the
     * top right widget width and the scrollbar width (if visible).
     *
     * @return {Integer} The right inset of the pane
     */
    getPaneInsetRight : function()
    {
      var topRight = this.getTopRightWidget();
      var topRightWidth =
        topRight && topRight.isVisible() && topRight.getBounds() ?
          topRight.getBounds().width + topRight.getMarginLeft() + topRight.getMarginRight() :
          0;

      var scrollBar = this.__verScrollBar;
      var scrollBarWidth = this.getVerticalScrollBarVisible() ?
        this.getVerticalScrollBarWidth() + scrollBar.getMarginLeft() + scrollBar.getMarginRight() :
        0;

      return Math.max(topRightWidth, scrollBarWidth);
    },


    /**
     * Set the pane's width
     *
     * @param width {Integer} The pane's width
     */
    setPaneWidth : function(width)
    {
      if (this.isVerticalScrollBarVisible()) {
        width += this.getPaneInsetRight();
      }
      this.setWidth(width);
    },


    // overridden
    _createChildControlImpl : function(id, hash)
    {
      var control;

      switch(id)
      {
        case "header":
          control = (this.getTable().getNewTablePaneHeader())(this);
          break;

        case "pane":
          control = (this.getTable().getNewTablePane())(this);
          break;

        case "focus-indicator":
          control = new qx.ui.table.pane.FocusIndicator(this);
          control.setUserBounds(0, 0, 0, 0);
          control.setZIndex(1000);
          control.addListener("mouseup", this._onMouseupFocusIndicator, this);
          this.__paneClipper.add(control);
          control.show();             // must be active for editor to operate
          control.setDecorator(null); // it can be initially invisible, though.
          break;

        case "resize-line":
          control = new qx.ui.core.Widget();
          control.setUserBounds(0, 0, 0, 0);
          control.setZIndex(1000);
          this.__paneClipper.add(control);
          break;

        case "scrollbar-x":
          control = this._createScrollBar("horizontal").set({
            alignY: "bottom"
          });
          control.addListener("scroll", this._onScrollX, this);

          if (this.__clipperContainer != null) {
            control.setMinHeight(qx.ui.core.scroll.AbstractScrollArea.DEFAULT_SCROLLBAR_WIDTH);
            this.__clipperContainer.add(control, {bottom: 0, right: 0, left: 0});
          } else {
            this._add(control, {row: 2, column: 0});
          }
          break;

        case "scrollbar-y":
          control = this._createScrollBar("vertical");
          control.addListener("scroll", this._onScrollY, this);

          if (this.__clipperContainer != null) {
            this.__clipperContainer.add(control, {right: 0, bottom: 0, top: 0});
          } else {
            this._add(control, {row: 1, column: 1});
          }
          break;
      }

      return control || this.base(arguments, id);
    },


    // property modifier
    _applyHorizontalScrollBarVisible : function(value, old) {
      this.__horScrollBar.setVisibility(value ? "visible" : "excluded");
    },


    // property modifier
    _applyVerticalScrollBarVisible : function(value, old) {
      this.__verScrollBar.setVisibility(value ? "visible" : "excluded");
    },


    // property modifier
    _applyTablePaneModel : function(value, old)
    {
      if (old != null) {
        old.removeListener("modelChanged", this._onPaneModelChanged, this);
      }

      value.addListener("modelChanged", this._onPaneModelChanged, this);
    },


    // property modifier
    _applyShowCellFocusIndicator : function(value, old)
    {
      if(value) {
        this.__focusIndicator.setDecorator("table-scroller-focus-indicator");
        this._updateFocusIndicator();
      }
      else {
        if(this.__focusIndicator) {
          this.__focusIndicator.setDecorator(null);
        }
      }
    },


    /**
     * Get the current position of the vertical scroll bar.
     *
     * @return {Integer} The current scroll position.
     */
    getScrollY : function() {
      return this.__verScrollBar.getPosition();
    },


    /**
     * Set the current position of the vertical scroll bar.
     *
     * @param scrollY {Integer} The new scroll position.
     * @param renderSync {Boolean?false} Whether the table update should be
     *     performed synchonously.
     */
    setScrollY : function(scrollY, renderSync)
    {
      this.__verScrollBar.scrollTo(scrollY);
      if (renderSync) {
        this._updateContent();
      }
    },


    /**
     * Get the current position of the vertical scroll bar.
     *
     * @return {Integer} The current scroll position.
     */
    getScrollX : function() {
      return this.__horScrollBar.getPosition();
    },


    /**
     * Set the current position of the vertical scroll bar.
     *
     * @param scrollX {Integer} The new scroll position.
     */
    setScrollX : function(scrollX) {
      this.__horScrollBar.scrollTo(scrollX);
    },


    /**
     * Returns the table this scroller belongs to.
     *
     * @return {qx.ui.table.Table} the table.
     */
    getTable : function() {
      return this.__table;
    },


    /**
     * Event handler. Called when the visibility of a column has changed.
     */
    onColVisibilityChanged : function()
    {
      this.updateHorScrollBarMaximum();
      this._updateFocusIndicator();
    },


    /**
     * Sets the column width.
     *
     * @param col {Integer} the column to change the width for.
     * @param width {Integer} the new width.
     */
    setColumnWidth : function(col, width)
    {
      this.__header.setColumnWidth(col, width);
      this.__tablePane.setColumnWidth(col, width);

      var paneModel = this.getTablePaneModel();
      var x = paneModel.getX(col);

      if (x != -1)
      {
        // The change was in this scroller
        this.updateHorScrollBarMaximum();
        this._updateFocusIndicator();
      }
    },


    /**
     * Event handler. Called when the column order has changed.
     *
     */
    onColOrderChanged : function()
    {
      this.__header.onColOrderChanged();
      this.__tablePane.onColOrderChanged();

      this.updateHorScrollBarMaximum();
    },


    /**
     * Event handler. Called when the table model has changed.
     *
     * @param firstRow {Integer} The index of the first row that has changed.
     * @param lastRow {Integer} The index of the last row that has changed.
     * @param firstColumn {Integer} The model index of the first column that has changed.
     * @param lastColumn {Integer} The model index of the last column that has changed.
     */
    onTableModelDataChanged : function(firstRow, lastRow, firstColumn, lastColumn)
    {
      this.__tablePane.onTableModelDataChanged(firstRow, lastRow, firstColumn, lastColumn);
      var rowCount = this.getTable().getTableModel().getRowCount();

      if (rowCount != this.__lastRowCount)
      {
        this.updateVerScrollBarMaximum();

        if (this.getFocusedRow() >= rowCount)
        {
          if (rowCount == 0) {
            this.setFocusedCell(null, null);
          } else {
            this.setFocusedCell(this.getFocusedColumn(), rowCount - 1);
          }
        }
        this.__lastRowCount = rowCount;
      }
    },


    /**
     * Event handler. Called when the selection has changed.
     */
    onSelectionChanged : function() {
      this.__tablePane.onSelectionChanged();
    },


    /**
     * Event handler. Called when the table gets or looses the focus.
     */
    onFocusChanged : function() {
      this.__tablePane.onFocusChanged();
    },


    /**
     * Event handler. Called when the table model meta data has changed.
     *
     */
    onTableModelMetaDataChanged : function()
    {
      this.__header.onTableModelMetaDataChanged();
      this.__tablePane.onTableModelMetaDataChanged();
    },


    /**
     * Event handler. Called when the pane model has changed.
     */
    _onPaneModelChanged : function()
    {
      this.__header.onPaneModelChanged();
      this.__tablePane.onPaneModelChanged();
    },


    /**
     * Event listener for the pane clipper's resize event
     */
    _onResizePane : function()
    {
      this.updateHorScrollBarMaximum();
      this.updateVerScrollBarMaximum();

      // The height has changed -> Update content
      this._updateContent();
      this.__header._updateContent();
      this.__table._updateScrollBarVisibility();
    },


    /**
     * Updates the maximum of the horizontal scroll bar, so it corresponds to the
     * total width of the columns in the table pane.
     */
    updateHorScrollBarMaximum : function()
    {
      var paneSize = this.__paneClipper.getInnerSize();
      if (!paneSize) {
        // will be called on the next resize event again
        return;
      }
      var scrollSize = this.getTablePaneModel().getTotalWidth();

      var scrollBar = this.__horScrollBar;

      if (paneSize.width < scrollSize)
      {
        var max = Math.max(0, scrollSize - paneSize.width);

        scrollBar.setMaximum(max);
        scrollBar.setKnobFactor(paneSize.width / scrollSize);

        var pos = scrollBar.getPosition();
        scrollBar.setPosition(Math.min(pos, max));
      }
      else
      {
        scrollBar.setMaximum(0);
        scrollBar.setKnobFactor(1);
        scrollBar.setPosition(0);
      }
    },


    /**
     * Updates the maximum of the vertical scroll bar, so it corresponds to the
     * number of rows in the table.
     */
    updateVerScrollBarMaximum : function()
    {
      var paneSize = this.__paneClipper.getInnerSize();
      if (!paneSize) {
        // will be called on the next resize event again
        return;
      }

      var tableModel = this.getTable().getTableModel();
      var rowCount = tableModel.getRowCount();

      if (this.getTable().getKeepFirstVisibleRowComplete()) {
        rowCount += 1;
      }

      var rowHeight = this.getTable().getRowHeight();
      var scrollSize = rowCount * rowHeight;
      var scrollBar = this.__verScrollBar;

      if (paneSize.height < scrollSize)
      {
        var max = Math.max(0, scrollSize - paneSize.height);

        scrollBar.setMaximum(max);
        scrollBar.setKnobFactor(paneSize.height / scrollSize);

        var pos = scrollBar.getPosition();
        scrollBar.setPosition(Math.min(pos, max));
      }
      else
      {
        scrollBar.setMaximum(0);
        scrollBar.setKnobFactor(1);
        scrollBar.setPosition(0);
      }
    },


    /**
     * Event handler. Called when the table property "keepFirstVisibleRowComplete"
     * changed.
     */
    onKeepFirstVisibleRowCompleteChanged : function()
    {
      this.updateVerScrollBarMaximum();
      this._updateContent();
    },


    /**
     * Event handler for the scroller's appear event
     */
    _onAppear : function() {
      // after the Scroller appears we start the interval again
      this._startInterval(this.getScrollTimeout());
    },


    /**
     * Event handler for the disappear event
     */
    _onDisappear : function()
    {
      // before the scroller disappears we need to stop it
      this._stopInterval();
    },


    /**
     * Event handler. Called when the horizontal scroll bar moved.
     *
     * @param e {Map} the event.
     */
    _onScrollX : function(e)
    {
      var scrollLeft = e.getData();

      this.fireDataEvent("changeScrollX", scrollLeft, e.getOldData());
      this._headerClipper.scrollToX(scrollLeft);
      this.__paneClipper.scrollToX(scrollLeft);
    },


    /**
     * Event handler. Called when the vertical scroll bar moved.
     *
     * @param e {Map} the event.
     */
    _onScrollY : function(e)
    {
      this.fireDataEvent("changeScrollY", e.getData(), e.getOldData());
      this._postponedUpdateContent();
    },


    /**
     * Event handler. Called when the user moved the mouse wheel.
     *
     * @param e {Map} the event.
     */
    _onMousewheel : function(e)
    {
      var table = this.getTable();

      if (!table.getEnabled()) {
        return;
      }

      // vertical scrolling
      var delta = e.getWheelDelta("y");
      // normalize that at least one step is scrolled at a time
      if (delta > 0 && delta < 1) {
        delta = 1;
      } else if (delta < 0 && delta > -1) {
        delta = -1;
      }
      if (qx.event.handler.MouseEmulation.ON) {
        this.__verScrollBar.scrollBy(delta);
      } else {
        this.__verScrollBar.scrollBySteps(delta);
      }

      var scrolled = delta != 0 && !this.__isAtEdge(this.__verScrollBar, delta);

      // horizontal scrolling
      delta = e.getWheelDelta("x");
      // normalize that at least one step is scrolled at a time
      if (delta > 0 && delta < 1) {
        delta = 1;
      } else if (delta < 0 && delta > -1) {
        delta = -1;
      }
      if (qx.event.handler.MouseEmulation.ON) {
        this.__horScrollBar.scrollBy(delta);
      } else {
        this.__horScrollBar.scrollBySteps(delta);
      }

      // Update the focus
      if (this.__lastMousePageX && this.getFocusCellOnMouseMove()) {
        this._focusCellAtPagePos(this.__lastMousePageX, this.__lastMousePageY);
      }

      scrolled = scrolled || (delta != 0 && !this.__isAtEdge(this.__horScrollBar, delta));

      // pass the event to the parent if the scrollbar is at an edge
      if (scrolled) {
        e.stop();
      }
    },


    /**
     * Checks if the table has been scrolled.
     * @param scrollBar {qx.ui.core.scroll.IScrollBar} The scrollbar to check
     * @param delta {Number} The scroll delta.
     * @return {Boolean} <code>true</code>, if the scrolling is a the edge
     */
    __isAtEdge : function(scrollBar, delta) {
      var position = scrollBar.getPosition();
      return (delta < 0 && position <= 0) || (delta > 0 && position >= scrollBar.getMaximum());
    },


    /**
     * Common column resize logic.
     *
     * @param pageX {Integer} the current mouse x position.
     */
    __handleResizeColumn : function(pageX)
    {
      var table = this.getTable();
      // We are currently resizing -> Update the position
      var headerCell = this.__header.getHeaderWidgetAtColumn(this.__resizeColumn);
      var minColumnWidth = headerCell.getSizeHint().minWidth;

      var newWidth = Math.max(minColumnWidth, this.__lastResizeWidth + pageX - this.__lastResizeMousePageX);

      if (this.getLiveResize()) {
        var columnModel = table.getTableColumnModel();
        columnModel.setColumnWidth(this.__resizeColumn, newWidth, true);
      } else {
        this.__header.setColumnWidth(this.__resizeColumn, newWidth, true);

        var paneModel = this.getTablePaneModel();
        this._showResizeLine(paneModel.getColumnLeft(this.__resizeColumn) + newWidth);
      }

      this.__lastResizeMousePageX += newWidth - this.__lastResizeWidth;
      this.__lastResizeWidth = newWidth;
    },


    /**
     * Common column move logic.
     *
     * @param pageX {Integer} the current mouse x position.
     *
     */
    __handleMoveColumn : function(pageX)
    {
      // We are moving a column

      // Check whether we moved outside the click tolerance so we can start
      // showing the column move feedback
      // (showing the column move feedback prevents the onclick event)
      var clickTolerance = qx.ui.table.pane.Scroller.CLICK_TOLERANCE;
      if (this.__header.isShowingColumnMoveFeedback()
        || pageX > this.__lastMoveMousePageX + clickTolerance
        || pageX < this.__lastMoveMousePageX - clickTolerance)
      {
        this.__lastMoveColPos += pageX - this.__lastMoveMousePageX;

        this.__header.showColumnMoveFeedback(this._moveColumn, this.__lastMoveColPos);

        // Get the responsible scroller
        var targetScroller = this.__table.getTablePaneScrollerAtPageX(pageX);
        if (this._lastMoveTargetScroller && this._lastMoveTargetScroller != targetScroller) {
          this._lastMoveTargetScroller.hideColumnMoveFeedback();
        }
        if (targetScroller != null) {
          this._lastMoveTargetX = targetScroller.showColumnMoveFeedback(pageX);
        } else {
          this._lastMoveTargetX = null;
        }

        this._lastMoveTargetScroller = targetScroller;
        this.__lastMoveMousePageX = pageX;
      }
    },


    /**
     * Event handler. Called when the user moved the mouse over the header.
     *
     * @param e {Map} the event.
     */
    _onMousemoveHeader : function(e)
    {
      var table = this.getTable();

      if (! table.getEnabled()) {
        return;
      }

      var useResizeCursor = false;
      var mouseOverColumn = null;

      var pageX = e.getDocumentLeft();
      var pageY = e.getDocumentTop();

      // Workaround: In onmousewheel the event has wrong coordinates for pageX
      //       and pageY. So we remember the last move event.
      this.__lastMousePageX = pageX;
      this.__lastMousePageY = pageY;

      if (this.__resizeColumn != null)
      {
        // We are currently resizing -> Update the position
        this.__handleResizeColumn(pageX);
        useResizeCursor = true;
        e.stopPropagation();
      }
      else if (this._moveColumn != null)
      {
        // We are moving a column
        this.__handleMoveColumn(pageX);
        e.stopPropagation();
      }
      else
      {
        var resizeCol = this._getResizeColumnForPageX(pageX);
        if (resizeCol != -1)
        {
          // The mouse is over a resize region -> Show the right cursor
          useResizeCursor = true;
        }
        else
        {
          var tableModel = table.getTableModel();
          var col = this._getColumnForPageX(pageX);
          if (col != null && tableModel.isColumnSortable(col)) {
            mouseOverColumn = col;
          }
        }
      }

      var cursor = useResizeCursor ? "col-resize" : null;
      this.getApplicationRoot().setGlobalCursor(cursor);
      this.setCursor(cursor);
      this.__header.setMouseOverColumn(mouseOverColumn);
    },


    /**
     * Event handler. Called when the user moved the mouse over the pane.
     *
     * @param e {Map} the event.
     */
    _onMousemovePane : function(e)
    {
      var table = this.getTable();

      if (! table.getEnabled()) {
        return;
      }

      //var useResizeCursor = false;

      var pageX = e.getDocumentLeft();
      var pageY = e.getDocumentTop();

      // Workaround: In onmousewheel the event has wrong coordinates for pageX
      //       and pageY. So we remember the last move event.
      this.__lastMousePageX = pageX;
      this.__lastMousePageY = pageY;

      var row = this._getRowForPagePos(pageX, pageY);
      if (row != null && this._getColumnForPageX(pageX) != null) {
        // The mouse is over the data -> update the focus
        if (this.getFocusCellOnMouseMove()) {
          this._focusCellAtPagePos(pageX, pageY);
        }
      }
      this.__header.setMouseOverColumn(null);
    },


    /**
     * Event handler. Called when the user pressed a mouse button over the header.
     *
     * @param e {Map} the event.
     */
    _onMousedownHeader : function(e)
    {
      if (! this.getTable().getEnabled()) {
        return;
      }

      var pageX = e.getDocumentLeft();

      // mouse is in header
      var resizeCol = this._getResizeColumnForPageX(pageX);
      if (resizeCol != -1)
      {
        // The mouse is over a resize region -> Start resizing
        this._startResizeHeader(resizeCol, pageX);
        e.stop();
      }
      else
      {
        // The mouse is not in a resize region
        var moveCol = this._getColumnForPageX(pageX);
        if (moveCol != null)
        {
          this._startMoveHeader(moveCol, pageX);
          e.stop();
        }
      }
    },


    /**
     * Start a resize session of the header.
     *
     * @param resizeCol {Integer} the column index
     * @param pageX {Integer} x coordinate of the mouse event
     */
    _startResizeHeader : function(resizeCol, pageX)
    {
      var columnModel = this.getTable().getTableColumnModel();

      // The mouse is over a resize region -> Start resizing
      this.__resizeColumn = resizeCol;
      this.__lastResizeMousePageX = pageX;
      this.__lastResizeWidth = columnModel.getColumnWidth(this.__resizeColumn);
      this._headerClipper.capture();
    },


    /**
     * Start a move session of the header.
     *
     * @param moveCol {Integer} the column index
     * @param pageX {Integer} x coordinate of the mouse event
     */
    _startMoveHeader : function(moveCol, pageX)
    {
      // Prepare column moving
      this._moveColumn = moveCol;
      this.__lastMoveMousePageX = pageX;
      this.__lastMoveColPos = this.getTablePaneModel().getColumnLeft(moveCol);
      this._headerClipper.capture();
    },



    /**
     * Event handler. Called when the user pressed a mouse button over the pane.
     *
     * @param e {Map} the event.
     */
    _onMousedownPane : function(e)
    {
      var table = this.getTable();

      if (! table.getEnabled()) {
        return;
      }

      if (table.isEditing()) {
        table.stopEditing();
      }

      var pageX = e.getDocumentLeft();
      var pageY = e.getDocumentTop();
      var row = this._getRowForPagePos(pageX, pageY);
      var col = this._getColumnForPageX(pageX);

      if (row !== null)
      {
        // The focus indicator blocks the click event on the scroller so we
        // store the current cell and listen for the mouseup event on the
        // focus indicator
        //
        // INVARIANT:
        //  The members of this object always contain the last position of
        //  the cell on which the mousedown event occurred.
        //  *** These values are never cleared! ***.
        //  Different browsers/OS combinations issue events in different
        //  orders, and the context menu event, in particular, can be issued
        //  early or late (Firefox on Linux issues it early; Firefox on
        //  Windows issues it late) so no one may clear these values.
        //
        this.__lastMouseDownCell = {
          row : row,
          col : col
        };

        // On the other hand, we need to know if we've issued the click event
        // so we don't issue it twice, both from mouse-up on the focus
        // indicator, and from the click even on the pane. Both possibilities
        // are necessary, however, to maintain the qooxdoo order of events.
        this.__firedClickEvent = false;

        var selectBeforeFocus = this.getSelectBeforeFocus();

        if (selectBeforeFocus) {
          table.getSelectionManager().handleMouseDown(row, e);
        }

        // The mouse is over the data -> update the focus
        if (! this.getFocusCellOnMouseMove()) {
          this._focusCellAtPagePos(pageX, pageY);
        }

        if (! selectBeforeFocus) {
          table.getSelectionManager().handleMouseDown(row, e);
        }
      }
    },


    /**
     * Event handler for the focus indicator's mouseup event
     *
     * @param e {qx.event.type.Mouse} The mouse event
     */
    _onMouseupFocusIndicator : function(e)
    {
      if (this.__lastMouseDownCell &&
          !this.__firedClickEvent &&
          !this.isEditing() &&
          this.__focusIndicator.getRow() == this.__lastMouseDownCell.row &&
          this.__focusIndicator.getColumn() == this.__lastMouseDownCell.col)
      {
        this.fireEvent("cellClick",
                       qx.ui.table.pane.CellEvent,
                       [
                         this,
                         e,
                         this.__lastMouseDownCell.row,
                         this.__lastMouseDownCell.col
                       ],
                       true);
        this.__firedClickEvent = true;
      } else if (!this.isEditing()) {
        // if no cellClick event should be fired, act like a mousedown which
        // invokes the change of the selection e.g. [BUG #1632]
        this._onMousedownPane(e);
      }
    },


    /**
     * Event handler. Called when the event capturing of the header changed.
     * Stops/finishes an active header resize/move session if it lost capturing
     * during the session to stay in a stable state.
     *
     * @param e {qx.event.type.Data} The data event
     */
    _onChangeCaptureHeader : function(e)
    {
      if (this.__resizeColumn != null) {
        this._stopResizeHeader();
      }

      if (this._moveColumn != null) {
        this._stopMoveHeader();
      }
    },


    /**
     * Stop a resize session of the header.
     *
     */
    _stopResizeHeader : function()
    {
      var columnModel = this.getTable().getTableColumnModel();

      // We are currently resizing -> Finish resizing
      if (! this.getLiveResize()) {
        this._hideResizeLine();
        columnModel.setColumnWidth(this.__resizeColumn,
                                   this.__lastResizeWidth,
                                   true);
      }

      this.__resizeColumn = null;
      this._headerClipper.releaseCapture();

      this.getApplicationRoot().setGlobalCursor(null);
      this.setCursor(null);

      // handle edit cell if available
      if (this.isEditing()) {
        var height = this.__cellEditor.getBounds().height;
        this.__cellEditor.setUserBounds(0, 0, this.__lastResizeWidth, height);
      }
    },


    /**
     * Stop a move session of the header.
     *
     */
    _stopMoveHeader : function()
    {
      var columnModel = this.getTable().getTableColumnModel();
      var paneModel = this.getTablePaneModel();

      // We are moving a column -> Drop the column
      this.__header.hideColumnMoveFeedback();
      if (this._lastMoveTargetScroller) {
        this._lastMoveTargetScroller.hideColumnMoveFeedback();
      }

      if (this._lastMoveTargetX != null)
      {
        var fromVisXPos = paneModel.getFirstColumnX() + paneModel.getX(this._moveColumn);
        var toVisXPos = this._lastMoveTargetX;
        if (toVisXPos != fromVisXPos && toVisXPos != fromVisXPos + 1)
        {
          // The column was really moved to another position
          // (and not moved before or after itself, which is a noop)

          // Translate visible positions to overall positions
          var fromCol = columnModel.getVisibleColumnAtX(fromVisXPos);
          var toCol   = columnModel.getVisibleColumnAtX(toVisXPos);
          var fromOverXPos = columnModel.getOverallX(fromCol);
          var toOverXPos = (toCol != null) ? columnModel.getOverallX(toCol) : columnModel.getOverallColumnCount();

          if (toOverXPos > fromOverXPos) {
            // Don't count the column itself
            toOverXPos--;
          }

          // Move the column
          columnModel.moveColumn(fromOverXPos, toOverXPos);

          // update the focus indicator including the editor
          this._updateFocusIndicator();
        }
      }

      this._moveColumn = null;
      this._lastMoveTargetX = null;
      this._headerClipper.releaseCapture();
    },


    /**
     * Event handler. Called when the user released a mouse button over the pane.
     *
     * @param e {Map} the event.
     */
    _onMouseupPane : function(e)
    {
      var table = this.getTable();

      if (! table.getEnabled()) {
        return;
      }

      var row = this._getRowForPagePos(e.getDocumentLeft(), e.getDocumentTop());
      if (row != -1 && row != null && this._getColumnForPageX(e.getDocumentLeft()) != null) {
        table.getSelectionManager().handleMouseUp(row, e);
      }
    },


    /**
     * Event handler. Called when the user released a mouse button over the header.
     *
     * @param e {Map} the event.
     */
    _onMouseupHeader : function(e)
    {
      var table = this.getTable();

      if (! table.getEnabled()) {
        return;
      }

      if (this.__resizeColumn != null)
      {
        this._stopResizeHeader();
        this.__ignoreClick = true;
        e.stop();
      }
      else if (this._moveColumn != null)
      {
        this._stopMoveHeader();
        e.stop();
      }
    },


    /**
     * Event handler. Called when the user clicked a mouse button over the header.
     *
     * @param e {Map} the event.
     */
    _onClickHeader : function(e)
    {
      if (this.__ignoreClick)
      {
        this.__ignoreClick = false;
        return;
      }

      var table = this.getTable();

      if (!table.getEnabled()) {
        return;
      }

      var tableModel = table.getTableModel();

      var pageX = e.getDocumentLeft();

      var resizeCol = this._getResizeColumnForPageX(pageX);

      if (resizeCol == -1)
      {
        // mouse is not in a resize region
        var col = this._getColumnForPageX(pageX);

        if (col != null && tableModel.isColumnSortable(col))
        {
          // Sort that column
          var sortCol = tableModel.getSortColumnIndex();
          var ascending = (col != sortCol) ? true : !tableModel.isSortAscending();

          var data =
            {
              column     : col,
              ascending  : ascending,
              clickEvent : e
            };

          if (this.fireDataEvent("beforeSort", data, null, true))
          {
            // Stop cell editing
            if (table.isEditing()) {
              table.stopEditing();
            }

            tableModel.sortByColumn(col, ascending);
            if (this.getResetSelectionOnHeaderClick())
            {
              table.getSelectionModel().resetSelection();
            }
          }
        }
      }

      e.stop();
    },


    /**
     * Event handler. Called when the user clicked a mouse button over the pane.
     *
     * @param e {Map} the event.
     */
    _onClickPane : function(e)
    {
      var table = this.getTable();

      if (!table.getEnabled()) {
        return;
      }

      var pageX = e.getDocumentLeft();
      var pageY = e.getDocumentTop();
      var row = this._getRowForPagePos(pageX, pageY);
      var col = this._getColumnForPageX(pageX);

      if (row != null && col != null)
      {
        table.getSelectionManager().handleClick(row, e);

        if (this.__focusIndicator.isHidden() ||
            (this.__lastMouseDownCell &&
             !this.__firedClickEvent &&
             !this.isEditing() &&
             row == this.__lastMouseDownCell.row &&
             col == this.__lastMouseDownCell.col))
        {
          this.fireEvent("cellClick",
                         qx.ui.table.pane.CellEvent,
                         [this, e, row, col],
                         true);
          this.__firedClickEvent = true;
        }
      }
    },


    /**
     * Event handler. Called when a context menu is invoked in a cell.
     *
     * @param e {qx.event.type.Mouse} the event.
     */
    _onContextMenu : function(e)
    {
      var pageX = e.getDocumentLeft();
      var pageY = e.getDocumentTop();
      var row = this._getRowForPagePos(pageX, pageY);
      var col = this._getColumnForPageX(pageX);

      /*
       * The 'row' value will be null if the right-click was in the blank
       * area below the last data row. Some applications desire to receive
       * the context menu event anyway, and can set the property value of
       * contextMenuFromDataCellsOnly to false to achieve that.
       */
      if (row === null && this.getContextMenuFromDataCellsOnly())
      {
        return;
      }

      if (! this.getShowCellFocusIndicator() ||
          row === null ||
          (this.__lastMouseDownCell &&
           row == this.__lastMouseDownCell.row &&
           col == this.__lastMouseDownCell.col))
      {
        this.fireEvent("cellContextmenu",
                       qx.ui.table.pane.CellEvent,
                       [this, e, row, col],
                       true);

        // Now that the cellContextmenu handler has had a chance to build
        // the menu for this cell, display it (if there is one).
        var menu = this.getTable().getContextMenu();
        if (menu)
        {
          // A menu with no children means don't display any context menu
          // including the default context menu even if the default context
          // menu is allowed to be displayed normally. There's no need to
          // actually show an empty menu, though.
          if (menu.getChildren().length > 0) {
            menu.openAtMouse(e);
          }
          else
          {
            menu.exclude();
          }

          // Do not show native menu
          e.preventDefault();
        }
      }
    },


    // overridden
    _onContextMenuOpen : function(e)
    {
      // This is Widget's context menu handler which typically retrieves
      // and displays the menu as soon as it receives a "contextmenu" event.
      // We want to allow the cellContextmenu handler to create the menu,
      // so we'll override this method with a null one, and do the menu
      // placement and display handling in our _onContextMenu method.
    },


    /**
     * Event handler. Called when the user double clicked a mouse button over the pane.
     *
     * @param e {Map} the event.
     */
    _onDblclickPane : function(e)
    {
      var pageX = e.getDocumentLeft();
      var pageY = e.getDocumentTop();


      this._focusCellAtPagePos(pageX, pageY);
      this.startEditing();

      var row = this._getRowForPagePos(pageX, pageY);
      if (row != -1 && row != null) {
        this.fireEvent("cellDblclick", qx.ui.table.pane.CellEvent, [this, e, row], true);
      }
    },


    /**
     * Event handler. Called when the mouse moved out.
     *
     * @param e {Map} the event.
     */
    _onMouseout : function(e)
    {
      var table = this.getTable();

      if (!table.getEnabled()) {
        return;
      }

      // Reset the resize cursor when the mouse leaves the header
      // If currently a column is resized then do nothing
      // (the cursor will be reset on mouseup)
      if (this.__resizeColumn == null)
      {
        this.setCursor(null);
        this.getApplicationRoot().setGlobalCursor(null);
      }

      this.__header.setMouseOverColumn(null);

      // in case the focus follows the mouse, it should be remove on mouseout
      if (this.getFocusCellOnMouseMove()) {
        this.__table.setFocusedCell();
      }
    },


    /**
     * Shows the resize line.
     *
     * @param x {Integer} the position where to show the line (in pixels, relative to
     *      the left side of the pane).
     */
    _showResizeLine : function(x)
    {
      var resizeLine = this._showChildControl("resize-line");

      var width = resizeLine.getWidth();
      var paneBounds = this.__paneClipper.getBounds();
      resizeLine.setUserBounds(
        x - Math.round(width/2), 0, width, paneBounds.height
      );
    },


    /**
     * Hides the resize line.
     */
    _hideResizeLine : function() {
      this._excludeChildControl("resize-line");
    },


    /**
     * Shows the feedback shown while a column is moved by the user.
     *
     * @param pageX {Integer} the x position of the mouse in the page (in pixels).
     * @return {Integer} the visible x position of the column in the whole table.
     */
    showColumnMoveFeedback : function(pageX)
    {
      var paneModel = this.getTablePaneModel();
      var columnModel = this.getTable().getTableColumnModel();
      var paneLeft = this.__tablePane.getContentLocation().left;
      var colCount = paneModel.getColumnCount();

      var targetXPos = 0;
      var targetX = 0;
      var currX = paneLeft;

      for (var xPos=0; xPos<colCount; xPos++)
      {
        var col = paneModel.getColumnAtX(xPos);
        var colWidth = columnModel.getColumnWidth(col);

        if (pageX < currX + colWidth / 2) {
          break;
        }

        currX += colWidth;
        targetXPos = xPos + 1;
        targetX = currX - paneLeft;
      }

      // Ensure targetX is visible
      var scrollerLeft = this.__paneClipper.getContentLocation().left;
      var scrollerWidth = this.__paneClipper.getBounds().width;
      var scrollX = scrollerLeft - paneLeft;

      // NOTE: +2/-1 because of feedback width
      targetX = qx.lang.Number.limit(targetX, scrollX + 2, scrollX + scrollerWidth - 1);

      this._showResizeLine(targetX);

      // Return the overall target x position
      return paneModel.getFirstColumnX() + targetXPos;
    },


    /**
     * Hides the feedback shown while a column is moved by the user.
     */
    hideColumnMoveFeedback : function() {
      this._hideResizeLine();
    },


    /**
     * Sets the focus to the cell that's located at the page position
     * <code>pageX</code>/<code>pageY</code>. If there is no cell at that position,
     * nothing happens.
     *
     * @param pageX {Integer} the x position in the page (in pixels).
     * @param pageY {Integer} the y position in the page (in pixels).
     */
    _focusCellAtPagePos : function(pageX, pageY)
    {
      var row = this._getRowForPagePos(pageX, pageY);

      if (row != -1 && row != null)
      {
        // The mouse is over the data -> update the focus
        var col = this._getColumnForPageX(pageX);
        this.__table.setFocusedCell(col, row);
      }
    },


    /**
     * Sets the currently focused cell.
     *
     * @param col {Integer} the model index of the focused cell's column.
     * @param row {Integer} the model index of the focused cell's row.
     */
    setFocusedCell : function(col, row)
    {
      if (!this.isEditing())
      {
        this.__tablePane.setFocusedCell(col, row, this.__updateContentPlanned);

        this.__focusedCol = col;
        this.__focusedRow = row;

        this._updateFocusIndicator();
      }
    },


    /**
     * Returns the column of currently focused cell.
     *
     * @return {Integer} the model index of the focused cell's column.
     */
    getFocusedColumn : function() {
      return this.__focusedCol;
    },


    /**
     * Returns the row of currently focused cell.
     *
     * @return {Integer} the model index of the focused cell's column.
     */
    getFocusedRow : function() {
      return this.__focusedRow;
    },


    /**
     * Scrolls a cell visible.
     *
     * @param col {Integer} the model index of the column the cell belongs to.
     * @param row {Integer} the model index of the row the cell belongs to.
     */
    scrollCellVisible : function(col, row)
    {
      var paneModel = this.getTablePaneModel();
      var xPos = paneModel.getX(col);

      if (xPos != -1)
      {
        var clipperSize = this.__paneClipper.getInnerSize();
        if (!clipperSize) {
          return;
        }

        var columnModel = this.getTable().getTableColumnModel();

        var colLeft = paneModel.getColumnLeft(col);
        var colWidth = columnModel.getColumnWidth(col);
        var rowHeight = this.getTable().getRowHeight();
        var rowTop = row * rowHeight;

        var scrollX = this.getScrollX();
        var scrollY = this.getScrollY();

        // NOTE: We don't use qx.lang.Number.limit, because min should win if max < min
        var minScrollX = Math.min(colLeft, colLeft + colWidth - clipperSize.width);
        var maxScrollX = colLeft;
        this.setScrollX(Math.max(minScrollX, Math.min(maxScrollX, scrollX)));

        var minScrollY = rowTop + rowHeight - clipperSize.height;

        if (this.getTable().getKeepFirstVisibleRowComplete()) {
          minScrollY += rowHeight;
        }

        var maxScrollY = rowTop;
        this.setScrollY(Math.max(minScrollY, Math.min(maxScrollY, scrollY)), true);
      }
    },


    /**
     * Returns whether currently a cell is editing.
     *
     * @return {var} whether currently a cell is editing.
     */
    isEditing : function() {
      return this.__cellEditor != null;
    },


    /**
     * Starts editing the currently focused cell. Does nothing if already
     * editing, if the column is not editable, or if the cell editor for the
     * column ascertains that the particular cell is not editable.
     *
     * @return {Boolean} whether editing was started
     */
    startEditing : function()
    {
      var table = this.getTable();
      var tableModel = table.getTableModel();
      var col = this.__focusedCol;

      if (
        !this.isEditing() &&
        (col != null) &&
        tableModel.isColumnEditable(col)
      ) {
        var row = this.__focusedRow;
        var xPos = this.getTablePaneModel().getX(col);
        var value = tableModel.getValue(col, row);

        // scroll cell into view
        this.scrollCellVisible(xPos, row);

        this.__cellEditorFactory = table.getTableColumnModel().getCellEditorFactory(col);

        var cellInfo =
        {
          col   : col,
          row   : row,
          xPos  : xPos,
          value : value,
          table : table
        };

        // Get a cell editor
        this.__cellEditor = this.__cellEditorFactory.createCellEditor(cellInfo);

        // We handle two types of cell editors: the traditional in-place
        // editor, where the cell editor returned by the factory must fit in
        // the space of the table cell; and a modal window in which the
        // editing takes place.  Additionally, if the cell editor determines
        // that it does not want to edit the particular cell being requested,
        // it may return null to indicate that that cell is not editable.
        if (this.__cellEditor === null)
        {
          // This cell is not editable even though its column is.
          return false;
        }
        else if (this.__cellEditor instanceof qx.ui.window.Window)
        {
          // It's a window.  Ensure that it's modal.
          this.__cellEditor.setModal(true);

          // At least for the time being, we disallow the close button.  It
          // acts differently than a cellEditor.close(), and invokes a bug
          // someplace.  Modal window cell editors should provide their own
          // buttons or means to activate a cellEditor.close() or equivalently
          // cellEditor.hide().
          this.__cellEditor.setShowClose(false);

          // Arrange to be notified when it is closed.
          this.__cellEditor.addListener(
            "close",
            this._onCellEditorModalWindowClose,
            this);

          // If there's a pre-open function defined for the table...
          var f = table.getModalCellEditorPreOpenFunction();
          if (f != null) {
            f(this.__cellEditor, cellInfo);
          }

          // Open it now.
          this.__cellEditor.open();
        }
        else
        {
          // The cell editor is a traditional in-place editor.
          var size = this.__focusIndicator.getInnerSize();
          this.__cellEditor.setUserBounds(0, 0, size.width, size.height);

          // prevent click event from bubbling up to the table
          this.__focusIndicator.addListener("mousedown", function(e)
          {
            this.__lastMouseDownCell = {
              row : this.__focusedRow,
              col : this.__focusedCol
            };
            e.stopPropagation();
          }, this);

          this.__focusIndicator.add(this.__cellEditor);
          this.__focusIndicator.addState("editing");
          this.__focusIndicator.setKeepActive(false);

          // Make the focus indicator visible during editing
          this.__focusIndicator.setDecorator("table-scroller-focus-indicator");

          this.__cellEditor.focus();
          this.__cellEditor.activate();
        }

        return true;
      }

      return false;
    },


    /**
     * Stops editing and writes the editor's value to the model.
     */
    stopEditing : function()
    {
      // If the focus indicator is not being shown normally...
      if (! this.getShowCellFocusIndicator())
      {
        // ... then hide it again
        this.__focusIndicator.setDecorator(null);
      }

      this.flushEditor();
      this.cancelEditing();
    },


    /**
     * Writes the editor's value to the model.
     */
    flushEditor : function()
    {
      if (this.isEditing())
      {
        var value = this.__cellEditorFactory.getCellEditorValue(this.__cellEditor);
        var oldValue = this.getTable().getTableModel().getValue(this.__focusedCol, this.__focusedRow);
        this.getTable().getTableModel().setValue(this.__focusedCol, this.__focusedRow, value);

        this.__table.focus();

        // Fire an event containing the value change.
        this.__table.fireDataEvent("dataEdited",
                                   {
                                     row      : this.__focusedRow,
                                     col      : this.__focusedCol,
                                     oldValue : oldValue,
                                     value    : value
                                   });
      }
    },


    /**
     * Stops editing without writing the editor's value to the model.
     */
    cancelEditing : function()
    {
      if (this.isEditing() && ! this.__cellEditor.pendingDispose)
      {
        if (this._cellEditorIsModalWindow)
        {
          this.__cellEditor.destroy();
          this.__cellEditor = null;
          this.__cellEditorFactory = null;
          this.__cellEditor.pendingDispose = true;
        }
        else
        {
          this.__focusIndicator.removeState("editing");
          this.__focusIndicator.setKeepActive(true);
          this.__cellEditor.destroy();
          this.__cellEditor = null;
          this.__cellEditorFactory = null;
        }
      }
    },


    /**
     * Event handler. Called when the modal window of the cell editor closes.
     *
     * @param e {Map} the event.
     */
    _onCellEditorModalWindowClose : function(e) {
      this.stopEditing();
    },


    /**
     * Returns the model index of the column the mouse is over or null if the mouse
     * is not over a column.
     *
     * @param pageX {Integer} the x position of the mouse in the page (in pixels).
     * @return {Integer} the model index of the column the mouse is over.
     */
    _getColumnForPageX : function(pageX)
    {
      var columnModel = this.getTable().getTableColumnModel();
      var paneModel = this.getTablePaneModel();
      var colCount = paneModel.getColumnCount();
      var currX = this.__tablePane.getContentLocation().left;

      for (var x=0; x<colCount; x++)
      {
        var col = paneModel.getColumnAtX(x);
        var colWidth = columnModel.getColumnWidth(col);
        currX += colWidth;

        if (pageX < currX) {
          return col;
        }
      }

      return null;
    },


    /**
     * Returns the model index of the column that should be resized when dragging
     * starts here. Returns -1 if the mouse is in no resize region of any column.
     *
     * @param pageX {Integer} the x position of the mouse in the page (in pixels).
     * @return {Integer} the column index.
     */
    _getResizeColumnForPageX : function(pageX)
    {
      var columnModel = this.getTable().getTableColumnModel();
      var paneModel = this.getTablePaneModel();
      var colCount = paneModel.getColumnCount();
      var currX = this.__header.getContentLocation().left;
      var regionRadius = qx.ui.table.pane.Scroller.RESIZE_REGION_RADIUS;

      for (var x=0; x<colCount; x++)
      {
        var col = paneModel.getColumnAtX(x);
        var colWidth = columnModel.getColumnWidth(col);
        currX += colWidth;

        if (pageX >= (currX - regionRadius) && pageX <= (currX + regionRadius)) {
          return col;
        }
      }

      return -1;
    },


    /**
     * Returns the model index of the row the mouse is currently over. Returns -1 if
     * the mouse is over the header. Returns null if the mouse is not over any
     * column.
     *
     * @param pageX {Integer} the mouse x position in the page.
     * @param pageY {Integer} the mouse y position in the page.
     * @return {Integer} the model index of the row the mouse is currently over.
     */
    _getRowForPagePos : function(pageX, pageY)
    {
      var panePos = this.__tablePane.getContentLocation();

      if (pageX < panePos.left || pageX > panePos.right)
      {
        // There was no cell or header cell hit
        return null;
      }

      if (pageY >= panePos.top && pageY <= panePos.bottom)
      {
        // This event is in the pane -> Get the row
        var rowHeight = this.getTable().getRowHeight();

        var scrollY = this.__verScrollBar.getPosition();

        if (this.getTable().getKeepFirstVisibleRowComplete()) {
          scrollY = Math.floor(scrollY / rowHeight) * rowHeight;
        }

        var tableY = scrollY + pageY - panePos.top;
        var row = Math.floor(tableY / rowHeight);

        var tableModel = this.getTable().getTableModel();
        var rowCount = tableModel.getRowCount();

        return (row < rowCount) ? row : null;
      }

      var headerPos = this.__header.getContentLocation();

      if (
        pageY >= headerPos.top &&
        pageY <= headerPos.bottom &&
        pageX <= headerPos.right)
      {
        // This event is in the pane -> Return -1 for the header
        return -1;
      }

      return null;
    },


    /**
     * Sets the widget that should be shown in the top right corner.
     *
     * The widget will not be disposed, when this table scroller is disposed. So the
     * caller has to dispose it.
     *
     * @param widget {qx.ui.core.Widget} The widget to set. May be null.
     */
    setTopRightWidget : function(widget)
    {
      var oldWidget = this.__topRightWidget;

      if (oldWidget != null) {
        this.__top.remove(oldWidget);
      }

      if (widget != null) {
        this.__top.add(widget);
      }

      this.__topRightWidget = widget;
    },


    /**
     * Get the top right widget
     *
     * @return {qx.ui.core.Widget} The top right widget.
     */
    getTopRightWidget : function() {
      return this.__topRightWidget;
    },


    /**
     * Returns the header.
     *
     * @return {qx.ui.table.pane.Header} the header.
     */
    getHeader : function() {
      return this.__header;
    },


    /**
     * Returns the table pane.
     *
     * @return {qx.ui.table.pane.Pane} the table pane.
     */
    getTablePane : function() {
      return this.__tablePane;
    },


    /**
     * Get the rendered width of the vertical scroll bar. The return value is
     * <code>0</code> if the scroll bar is invisible or not yet rendered.
     *
     * @internal
     * @return {Integer} The width of the vertical scroll bar
     */
    getVerticalScrollBarWidth : function()
    {
      var scrollBar = this.__verScrollBar;
      return scrollBar.isVisible() ? (scrollBar.getSizeHint().width || 0) : 0;
    },


    /**
     * Returns which scrollbars are needed.
     *
     * @param forceHorizontal {Boolean ? false} Whether to show the horizontal
     *      scrollbar always.
     * @param preventVertical {Boolean ? false} Whether to show the vertical scrollbar
     *      never.
     * @return {Integer} which scrollbars are needed. This may be any combination of
     *      {@link #HORIZONTAL_SCROLLBAR} or {@link #VERTICAL_SCROLLBAR}
     *      (combined by OR).
     */
    getNeededScrollBars : function(forceHorizontal, preventVertical)
    {
      var verScrollBar = this.__verScrollBar;
      var verBarWidth = verScrollBar.getSizeHint().width
        + verScrollBar.getMarginLeft() + verScrollBar.getMarginRight();

      var horScrollBar = this.__horScrollBar;
      var horBarHeight = horScrollBar.getSizeHint().height
        + horScrollBar.getMarginTop() + horScrollBar.getMarginBottom();

      // Get the width and height of the view (without scroll bars)
      var clipperSize = this.__paneClipper.getInnerSize();
      var viewWidth = clipperSize ? clipperSize.width : 0;

      if (this.getVerticalScrollBarVisible()) {
        viewWidth += verBarWidth;
      }

      var viewHeight = clipperSize ? clipperSize.height : 0;

      if (this.getHorizontalScrollBarVisible()) {
        viewHeight += horBarHeight;
      }

      var tableModel = this.getTable().getTableModel();
      var rowCount = tableModel.getRowCount();

      // Get the (virtual) width and height of the pane
      var paneWidth = this.getTablePaneModel().getTotalWidth();
      var paneHeight = this.getTable().getRowHeight() * rowCount;

      // Check which scrollbars are needed
      var horNeeded = false;
      var verNeeded = false;

      if (paneWidth > viewWidth) {
        horNeeded = true;

        if (paneHeight > viewHeight - horBarHeight) {
          verNeeded = true;
        }
      } else if (paneHeight > viewHeight) {
        verNeeded = true;

        if (!preventVertical && (paneWidth > viewWidth - verBarWidth)) {
          horNeeded = true;
        }
      }

      // Create the mask
      var horBar = qx.ui.table.pane.Scroller.HORIZONTAL_SCROLLBAR;
      var verBar = qx.ui.table.pane.Scroller.VERTICAL_SCROLLBAR;
      return ((forceHorizontal || horNeeded) ? horBar : 0) | ((preventVertical || !verNeeded) ? 0 : verBar);
    },


    /**
     * Return the pane clipper. It is sometimes required for special activities
     * such as tracking events for drag&drop.
     *
     * @return {qx.ui.table.pane.Clipper}
     *   The pane clipper for this scroller.
     */
    getPaneClipper : function()
    {
      return this.__paneClipper;
    },


    /**
     * Returns the scroll area container widget (which enables more precise
     * operations e.g. bounds retrieval for drag session scrolling).
     *
     * @see qx.ui.core.MDragDropScrolling#_getBounds
     * @return {qx.ui.table.pane.Clipper}
     *   The pane clipper for this scroller.
     */
    getScrollAreaContainer : function() {
      return this.getPaneClipper();
    },


    // property apply method
    _applyScrollTimeout : function(value, old) {
      this._startInterval(value);
    },


    /**
     * Starts the current running interval
     *
     * @param timeout {Integer} The timeout between two table updates
     */
    _startInterval : function (timeout)
    {
      this.__timer.setInterval(timeout);
      this.__timer.start();
    },


    /**
     * stops the current running interval
     */
    _stopInterval : function ()
    {
      this.__timer.stop();
    },


    /**
     * Does a postponed update of the content.
     *
     * @see #_updateContent
     */
    _postponedUpdateContent : function()
    {
      //this.__updateContentPlanned = true;
      this._updateContent();
    },


    /**
     * Timer event handler. Periodically checks whether a table update is
     * required. The update interval is controlled by the {@link #scrollTimeout}
     * property.
     *
     * @signature function()
     */
    _oninterval : qx.event.GlobalError.observeMethod(function()
    {
      if (this.__updateContentPlanned && !this.__tablePane._layoutPending)
      {
        this.__updateContentPlanned = false;
        this._updateContent();
      }
    }),


    /**
     * Updates the content. Sets the right section the table pane should show and
     * does the scrolling.
     */
    _updateContent : function()
    {
      var paneSize = this.__paneClipper.getInnerSize();
      if (!paneSize) {
        return;
      }
      var paneHeight = paneSize.height;

      var scrollX = this.__horScrollBar.getPosition();
      var scrollY = this.__verScrollBar.getPosition();
      var rowHeight = this.getTable().getRowHeight();

      var firstRow = Math.floor(scrollY / rowHeight);
      var oldFirstRow = this.__tablePane.getFirstVisibleRow();
      this.__tablePane.setFirstVisibleRow(firstRow);

      var visibleRowCount = Math.ceil(paneHeight / rowHeight);
      var paneOffset = 0;
      var firstVisibleRowComplete = this.getTable().getKeepFirstVisibleRowComplete();

      if (!firstVisibleRowComplete)
      {

        // NOTE: We don't consider paneOffset, because this may cause alternating
        //       adding and deleting of one row when scrolling. Instead we add one row
        //       in every case.
        visibleRowCount++;

        paneOffset = scrollY % rowHeight;
      }

      this.__tablePane.setVisibleRowCount(visibleRowCount);

      if (firstRow != oldFirstRow) {
        this._updateFocusIndicator();
      }

      this.__paneClipper.scrollToX(scrollX);

      // Avoid expensive calls to setScrollTop if
      // scrolling is not needed
      if (! firstVisibleRowComplete ) {
        this.__paneClipper.scrollToY(paneOffset);
      }
    },

    /**
     * Updates the location and the visibility of the focus indicator.
     *
     */
    _updateFocusIndicator : function()
    {
      var table = this.getTable();

      if (!table.getEnabled()) {
        return;
      }

      this.__focusIndicator.moveToCell(this.__focusedCol, this.__focusedRow);
    }
  },




  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    this._stopInterval();

    // this object was created by the table on init so we have to clean it up.
    var tablePaneModel = this.getTablePaneModel();
    if (tablePaneModel)
    {
      tablePaneModel.dispose();
    }

    this.__lastMouseDownCell = this.__topRightWidget = this.__table = null;
    this._disposeObjects("__horScrollBar", "__verScrollBar",
                         "_headerClipper", "__paneClipper", "__focusIndicator",
                         "__header", "__tablePane", "__top", "__timer",
                         "__clipperContainer");
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * Clipping area for the table header and table pane.
 */
qx.Class.define("qx.ui.table.pane.Clipper",
{
  extend : qx.ui.container.Composite,

  construct : function()
  {
    this.base(arguments, new qx.ui.layout.Grow());
    this.setMinWidth(0);
  },

  members :
  {
    /**
     * Scrolls the element's content to the given left coordinate
     *
     * @param value {Integer} The vertical position to scroll to.
     */
    scrollToX : function(value) {
      this.getContentElement().scrollToX(value, false);
    },


    /**
     * Scrolls the element's content to the given top coordinate
     *
     * @param value {Integer} The horizontal position to scroll to.
     */
    scrollToY : function(value) {
      this.getContentElement().scrollToY(value, true);
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * The focus indicator widget
 */
qx.Class.define("qx.ui.table.pane.FocusIndicator",
{
  extend : qx.ui.container.Composite,

  /**
   * @param scroller {Scroller} The scroller, which contains this focus indicator
   */
  construct : function(scroller)
  {
    this.base(arguments);
    this.__scroller = scroller;

    this.setKeepActive(true);
    this.addListener("keypress", this._onKeyPress, this);
  },

  properties :
  {
    // overridden
    visibility :
    {
      refine : true,
      init : "excluded"
    },

    /** Table row, where the indicator is placed. */
    row : {
      check : "Integer",
      nullable : true
    },

    /** Table column, where the indicator is placed. */
    column : {
      check : "Integer",
      nullable : true
    }
  },

  members :
  {
    __scroller : null,


    /**
     * Keypress handler. Suppress all key events but "Enter" and "Escape"
     *
     * @param e {qx.event.type.KeySequence} key event
     */
    _onKeyPress : function(e)
    {
      var iden = e.getKeyIdentifier();
      if (iden !== "Escape" && iden !== "Enter") {
        e.stopPropagation();
      }
    },


    /**
     * Move the focus indicator to the given table cell.
     *
     * @param col {Integer?null} The table column
     * @param row {Integer?null} The table row
     */
    moveToCell : function(col, row)
    {
      // check if the focus indicator is shown and if the new column is
      // editable. if not, just exclude the incdicator because the mouse events
      // should go to the cell itself linke with HTML links [BUG #4250]
      if (
        !this.__scroller.getShowCellFocusIndicator() &&
        !this.__scroller.getTable().getTableModel().isColumnEditable(col)
      ) {
        this.exclude();
        return;
      } else {
        this.show();
      }

      if (col == null)
      {
        this.hide();
        this.setRow(null);
        this.setColumn(null);
      }
      else
      {
        var xPos = this.__scroller.getTablePaneModel().getX(col);

        if (xPos == -1)
        {
          this.hide();
          this.setRow(null);
          this.setColumn(null);
        }
        else
        {
          var table = this.__scroller.getTable();
          var columnModel = table.getTableColumnModel();
          var paneModel = this.__scroller.getTablePaneModel();

          var firstRow = this.__scroller.getTablePane().getFirstVisibleRow();
          var rowHeight = table.getRowHeight();

          this.setUserBounds(
              paneModel.getColumnLeft(col) - 2,
              (row - firstRow) * rowHeight - 2,
              columnModel.getColumnWidth(col) + 3,
              rowHeight + 3
          );
          this.show();

          this.setRow(row);
          this.setColumn(col);
        }
      }
    }
  },

  destruct : function () {
     this.__scroller = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * David Perez Carmona (david-perez)

************************************************************************ */

/**
 * A cell event instance contains all data for mouse events related to cells in
 * a table.
 **/
qx.Class.define("qx.ui.table.pane.CellEvent",
{
  extend : qx.event.type.Mouse,


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /** The table row of the event target */
    row :
    {
      check : "Integer",
      nullable: true
    },

    /** The table column of the event target */
    column :
    {
      check : "Integer",
      nullable: true
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
     *****************************************************************************
        CONSTRUCTOR
     *****************************************************************************
     */

     /**
      * Initialize the event
      *
      * @param scroller {qx.ui.table.pane.Scroller} The tables pane scroller
      * @param me {qx.event.type.Mouse} The original mouse event
      * @param row {Integer?null} The cell's row index
      * @param column {Integer?null} The cell's column index
      */
    init : function(scroller, me, row, column)
    {
      me.clone(this);
      this.setBubbles(false);

      if (row != null) {
        this.setRow(row);
      } else {
        this.setRow(scroller._getRowForPagePos(this.getDocumentLeft(), this.getDocumentTop()));
      }

      if (column != null) {
        this.setColumn(column);
      } else {
        this.setColumn(scroller._getColumnForPageX(this.getDocumentLeft()));
      }
    },


    // overridden
    clone : function(embryo)
    {
      var clone = this.base(arguments, embryo);

      clone.set({
        row: this.getRow(),
        column: this.getColumn()
      });

      return clone;
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)

************************************************************************ */

/**
 * Helper functions for numbers.
 *
 * The native JavaScript Number is not modified by this class.
 *
 */
qx.Class.define("qx.lang.Number",
{
  statics :
  {
    /**
     * Check whether the number is in a given range
     *
     * @param nr {Number} the number to check
     * @param vmin {Integer} lower bound of the range
     * @param vmax {Integer} upper bound of the range
     * @return {Boolean} whether the number is >= vmin and <= vmax
     */
    isInRange : function(nr, vmin, vmax) {
      return nr >= vmin && nr <= vmax;
    },


    /**
     * Check whether the number is between a given range
     *
     * @param nr {Number} the number to check
     * @param vmin {Integer} lower bound of the range
     * @param vmax {Integer} upper bound of the range
     * @return {Boolean} whether the number is > vmin and < vmax
     */
    isBetweenRange : function(nr, vmin, vmax) {
      return nr > vmin && nr < vmax;
    },


    /**
     * Limit the number to a given range
     *
     * * If the number is greater than the upper bound, the upper bound is returned
     * * If the number is smaller than the lower bound, the lower bound is returned
     * * If the number is in the range, the number is returned
     *
     * @param nr {Number} the number to limit
     * @param vmin {Integer} lower bound of the range
     * @param vmax {Integer} upper bound of the range
     * @return {Integer} the limited number
     */
    limit : function(nr, vmin, vmax)
    {
      if (vmax != null && nr > vmax) {
        return vmax;
      } else if (vmin != null && nr < vmin) {
        return vmin;
      } else {
        return nr;
      }
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2006 STZ-IDA, Germany, http://www.stz-ida.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Til Schneider (til132)

************************************************************************ */

/**
 * The model of a table pane. This model works as proxy to a
 * {@link qx.ui.table.columnmodel.Basic} and manages the visual order of the columns shown in
 * a {@link Pane}.
 */
qx.Class.define("qx.ui.table.pane.Model",
{
  extend : qx.core.Object,




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   *
   * @param tableColumnModel {qx.ui.table.columnmodel.Basic} The TableColumnModel of which this
   *    model is the proxy.
   */
  construct : function(tableColumnModel)
  {
    this.base(arguments);

    this.setTableColumnModel(tableColumnModel);
  },




  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events :
  {
    /** Fired when the model changed. */
    "modelChanged" : "qx.event.type.Event"
  },



  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

  statics :
  {

    /** @type {string} The type of the event fired when the model changed. */
    EVENT_TYPE_MODEL_CHANGED : "modelChanged"
  },




  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {

    /** The visible x position of the first column this model should contain. */
    firstColumnX :
    {
      check : "Integer",
      init : 0,
      apply : "_applyFirstColumnX"
    },


    /**
     * The maximum number of columns this model should contain. If -1 this model will
     * contain all remaining columns.
     */
    maxColumnCount :
    {
      check : "Number",
      init : -1,
      apply : "_applyMaxColumnCount"
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    __columnCount : null,
    __tableColumnModel : null,


    // property modifier
    _applyFirstColumnX : function(value, old)
    {
      this.__columnCount = null;
      this.fireEvent(qx.ui.table.pane.Model.EVENT_TYPE_MODEL_CHANGED);
    },

    // property modifier
    _applyMaxColumnCount : function(value, old)
    {
      this.__columnCount = null;
      this.fireEvent(qx.ui.table.pane.Model.EVENT_TYPE_MODEL_CHANGED);
    },


    /**
     * Connects the table model to the column model
     *
     * @param tableColumnModel {qx.ui.table.columnmodel.Basic} the column model
     */
    setTableColumnModel : function(tableColumnModel)
    {
      if (this.__tableColumnModel) {
        this.__tableColumnModel.removeListener("visibilityChangedPre", this._onColVisibilityChanged, this);
        this.__tableColumnModel.removeListener("headerCellRendererChanged", this._onColVisibilityChanged, this);
      }
      this.__tableColumnModel = tableColumnModel;
      this.__tableColumnModel.addListener("visibilityChangedPre", this._onColVisibilityChanged, this);
      this.__tableColumnModel.addListener("headerCellRendererChanged", this._onHeaderCellRendererChanged, this);
      this.__columnCount = null;
    },


    /**
     * Event handler. Called when the visibility of a column has changed.
     *
     * @param evt {Map} the event.
     */
    _onColVisibilityChanged : function(evt)
    {
      this.__columnCount = null;

      this.fireEvent(qx.ui.table.pane.Model.EVENT_TYPE_MODEL_CHANGED);
    },


    /**
     * Event handler. Called when the cell renderer of a column has changed.
     *
     * @param evt {Map} the event.
     */
    _onHeaderCellRendererChanged : function(evt)
    {
      this.fireEvent(qx.ui.table.pane.Model.EVENT_TYPE_MODEL_CHANGED);
    },


    /**
     * Returns the number of columns in this model.
     *
     * @return {Integer} the number of columns in this model.
     */
    getColumnCount : function()
    {
      if (this.__columnCount == null)
      {
        var firstX = this.getFirstColumnX();
        var maxColCount = this.getMaxColumnCount();
        var totalColCount = this.__tableColumnModel.getVisibleColumnCount();

        if (maxColCount == -1 || (firstX + maxColCount) > totalColCount) {
          this.__columnCount = totalColCount - firstX;
        } else {
          this.__columnCount = maxColCount;
        }
      }

      return this.__columnCount;
    },


    /**
     * Returns the model index of the column at the position <code>xPos</code>.
     *
     * @param xPos {Integer} the x position in the table pane of the column.
     * @return {Integer} the model index of the column.
     */
    getColumnAtX : function(xPos)
    {
      var firstX = this.getFirstColumnX();
      return this.__tableColumnModel.getVisibleColumnAtX(firstX + xPos);
    },


    /**
     * Returns the x position of the column <code>col</code>.
     *
     * @param col {Integer} the model index of the column.
     * @return {Integer} the x position in the table pane of the column.
     */
    getX : function(col)
    {
      var firstX = this.getFirstColumnX();
      var maxColCount = this.getMaxColumnCount();

      var x = this.__tableColumnModel.getVisibleX(col) - firstX;

      if (x >= 0 && (maxColCount == -1 || x < maxColCount)) {
        return x;
      } else {
        return -1;
      }
    },


    /**
     * Gets the position of the left side of a column (in pixels, relative to the
     * left side of the table pane).
     *
     * This value corresponds to the sum of the widths of all columns left of the
     * column.
     *
     * @param col {Integer} the model index of the column.
     * @return {var} the position of the left side of the column.
     */
    getColumnLeft : function(col)
    {
      var left = 0;
      var colCount = this.getColumnCount();

      for (var x=0; x<colCount; x++)
      {
        var currCol = this.getColumnAtX(x);

        if (currCol == col) {
          return left;
        }

        left += this.__tableColumnModel.getColumnWidth(currCol);
      }

      return -1;
    },


    /**
     * Returns the total width of all columns in the model.
     *
     * @return {Integer} the total width of all columns in the model.
     */
    getTotalWidth : function()
    {
      var totalWidth = 0;
      var colCount = this.getColumnCount();

      for (var x=0; x<colCount; x++)
      {
        var col = this.getColumnAtX(x);
        totalWidth += this.__tableColumnModel.getColumnWidth(col);
      }

      return totalWidth;
    }
  },




  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function() {
    if (this.__tableColumnModel)
    {
      this.__tableColumnModel.removeListener("visibilityChangedPre", this._onColVisibilityChanged, this);
      this.__tableColumnModel.removeListener("headerCellRendererChanged", this._onColVisibilityChanged, this);
    }
    this.__tableColumnModel = null;
  }
});
