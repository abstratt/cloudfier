/*
 This file only exists to explicitly state all dependencies for mobile Cloudfier applications.
 
 #asset(kirra/*)
 #asset(qx/mobile/js/iscroll.js)

 #require(qx.application.Mobile)
 #require(qx.application.Routing)
 #require(qx.core.Environment)
 #require(qx.core.Init)
 #require(qx.log.appender.Console)
 #require(qx.log.appender.Native)
 #require(qx.ui.mobile.basic.Label)
 #require(qx.ui.mobile.container.Drawer)
 #require(qx.ui.mobile.form.Form)
 #require(qx.ui.mobile.form.renderer.Single)
 #require(qx.ui.mobile.form.Button)
 #require(qx.ui.mobile.form.TextField)
 #require(qx.ui.mobile.form.PasswordField)
 #require(qx.ui.mobile.form.Title)
 #require(qx.ui.mobile.toolbar.ToolBar)
 #require(qx.ui.mobile.list.List)
 #require(qx.ui.mobile.page.Manager)
 #require(qx.ui.mobile.page.NavigationPage)
 #require(qx.ui.mobile.tabbar.TabBar)
 #require(qx.ui.mobile.tabbar.TabButton)
 #require(qx.data.controller.Form)
 #require(qx.data.controller.List)
 #require(qx.data.marshal.Json)
 #require(qx.data.store.Json)
 #require(qx.data.Array)
 #require(qx.event.type.Data)
 #require(qx.io.request.authentication.Basic)
 #require(qx.io.request.Xhr)
 #require(qx.bom.request.Script)
 #require(qx.lang.String)
 #require(qx.lang.Type)
 #require(qx.log.appender.Console)
 #require(qx.log.appender.Native)
 #require(qx.util.format.DateFormat)
 #require(qx.util.Request)
 #require(qx.util.Serializer)
 #require(qx.util.Validate)
*/

var cloudfier = cloudfier || {};
cloudfier.lib.showLoginWindow = function () {
    cloudfier.mobile.loginPage.show();
};
cloudfier.mobile = {
    loginPage : undefined
};


qx.Class.define("kirra.KirraClient", {
    // hit the server
    // in case of 401, show login dialog
    // in case of success, look for entities
});
