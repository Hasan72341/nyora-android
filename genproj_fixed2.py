import os, hashlib

ROOT = os.path.dirname(os.path.abspath(__file__))
APP_DIR = os.path.join(ROOT, "NyoraApp")
WIDGET_DIR = os.path.join(ROOT, "NyoraWidgets")
PROJ = os.path.join(ROOT, "NyoraApp.xcodeproj")

def oid(*parts):
    h = hashlib.sha1("::".join(parts).encode()).hexdigest().upper()
    return h[:24]

# Collect app sources
swift_files, resources = [], []
for dirpath, _dirs, files in os.walk(APP_DIR):
    for f in sorted(files):
        rel = os.path.relpath(os.path.join(dirpath, f), APP_DIR)
        if f.endswith(".swift"):
            swift_files.append(rel)
swift_files.sort()
resources.append("Assets.xcassets")
res_dir = os.path.join(APP_DIR, "Resources")
if os.path.isdir(res_dir):
    for f in sorted(os.listdir(res_dir)):
        if f.endswith(".js") or f.endswith(".json"):
            resources.append(f"Resources/{f}")
        elif f == "web" and os.path.isdir(os.path.join(res_dir, f)):
            resources.append("Resources/web")

widget_files = []
if os.path.isdir(WIDGET_DIR):
    for f in sorted(os.listdir(WIDGET_DIR)):
        if f.endswith(".swift"):
            widget_files.append(f)
widget_files.sort()

# IDs
PROJECT = oid("project")
MAIN_GROUP = oid("group", "main")
APP_GROUP = oid("group", "app")
WIDGET_GROUP = oid("group", "widget")
PRODUCTS_GROUP = oid("group", "products")
FRAMEWORKS_GROUP = oid("group", "frameworks")
TARGET = oid("target", "app")
PRODUCT_REF = oid("product", "app.app")
SRC_PHASE = oid("phase", "sources")
FW_PHASE = oid("phase", "frameworks")
RES_PHASE = oid("phase", "resources")
EMBED_PHASE = oid("embed-appex")
PROJ_CFG_LIST = oid("cfglist", "project")
TARGET_CFG_LIST = oid("cfglist", "target")
PROJ_DEBUG = oid("cfg", "proj", "debug")
PROJ_RELEASE = oid("cfg", "proj", "release")
TARGET_DEBUG = oid("cfg", "target", "debug")
TARGET_RELEASE = oid("cfg", "target", "release")
PKG_REF = oid("pkg", "kotatsu")
PKG_PRODUCT = oid("pkgproduct", "kotatsu")
PKG_BUILDFILE = oid("buildfile", "kotatsu")
INFO_PLIST_REF = oid("fileref", "Info.plist")
ENTITLEMENTS_REF = oid("fileref", "Nyora.entitlements")

GOOGLE_PKG_REF = oid("pkg", "googlesignin")
GOOGLE_PKG_PRODUCT = oid("pkgproduct", "googlesignin")
GOOGLE_BUILDFILE = oid("buildfile", "googlesignin")

W_TARGET = oid("target", "widget")
W_PRODUCT_REF = oid("product", "widget.appex")
W_SRC_PHASE = oid("phase", "widget-sources")
W_CFG_LIST = oid("cfglist", "widget")
W_DEBUG = oid("cfg", "widget", "debug")
W_RELEASE = oid("cfg", "widget", "release")
W_INFO_REF = oid("fileref", "widget.Info.plist")
W_DEP = oid("dep", "widget")
W_PROXY = oid("proxy", "widget")
W_EMBED_BUILDFILE = oid("buildfile", "embed-appex")

file_refs, build_files = {}, {}
for rel in swift_files + resources:
    file_refs[rel] = oid("fileref", rel)
    build_files[rel] = oid("buildfile", rel)
w_file_refs, w_build_files = {}, {}
for rel in widget_files:
    w_file_refs[rel] = oid("fileref", "widget", rel)
    w_build_files[rel] = oid("buildfile", "widget", rel)

def ftype(rel):
    if rel.endswith(".swift"): return "sourcecode.swift"
    if rel.endswith(".xcassets"): return "folder.assetcatalog"
    if rel.endswith(".plist"): return "text.plist.xml"
    if rel == "Resources/web": return "folder"
    return "text"

lines = []
def w(s=""): lines.append(s)

w("// !$*UTF8*$!")
w("{")
w("\tarchiveVersion = 1;")
w("\tclasses = {};")
w("\tobjectVersion = 56;")
w("\tobjects = {")

# PBXBuildFile
w("\n/* Begin PBXBuildFile section */")
for rel in swift_files:
    w(f"\t\t{build_files[rel]} /* {rel} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_refs[rel]} /* {rel} */; }};")
for rel in resources:
    w(f"\t\t{build_files[rel]} /* {rel} in Resources */ = {{isa = PBXBuildFile; fileRef = {file_refs[rel]} /* {rel} */; }};")
for rel in widget_files:
    w(f"\t\t{w_build_files[rel]} /* {rel} in Sources */ = {{isa = PBXBuildFile; fileRef = {w_file_refs[rel]} /* {rel} */; }};")
w(f"\t\t{PKG_BUILDFILE} /* NyoraEngine in Frameworks */ = {{isa = PBXBuildFile; productRef = {PKG_PRODUCT} /* NyoraEngine */; }};")
w(f"\t\t{GOOGLE_BUILDFILE} /* GoogleSignIn in Frameworks */ = {{isa = PBXBuildFile; productRef = {GOOGLE_PKG_PRODUCT} /* GoogleSignIn */; }};")
w(f"\t\t{W_EMBED_BUILDFILE} /* NyoraWidgetsExtension.appex in Embed Foundation Extensions */ = {{isa = PBXBuildFile; fileRef = {W_PRODUCT_REF} /* NyoraWidgetsExtension.appex */; settings = {{ATTRIBUTES = (RemoveHeadersOnCopy, ); }}; }};")
w("/* End PBXBuildFile section */")

# PBXContainerItemProxy
w("\n/* Begin PBXContainerItemProxy section */")
w(f"\t\t{W_PROXY} /* PBXContainerItemProxy */ = {{")
w("\t\t\tisa = PBXContainerItemProxy;")
w(f"\t\t\tcontainerPortal = {PROJECT} /* Project object */;")
w("\t\t\tproxyType = 1;")
w(f"\t\t\tremoteGlobalIDString = {W_TARGET};")
w("\t\t\tremoteInfo = NyoraWidgetsExtension;")
w("\t\t};")
w("/* End PBXContainerItemProxy section */")

# PBXCopyFilesBuildPhase (embed extension into app)
w("\n/* Begin PBXCopyFilesBuildPhase section */")
w(f"\t\t{EMBED_PHASE} /* Embed Foundation Extensions */ = {{")
w("\t\t\tisa = PBXCopyFilesBuildPhase;")
w("\t\t\tbuildActionMask = 2147483647;")
w("\t\t\tdstPath = \"\";")
w("\t\t\tdstSubfolderSpec = 13;")
w("\t\t\tfiles = (")
w(f"\t\t\t\t{W_EMBED_BUILDFILE} /* NyoraWidgetsExtension.appex in Embed Foundation Extensions */,")
w("\t\t\t);")
w("\t\t\tname = \"Embed Foundation Extensions\";")
w("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
w("\t\t};")
w("/* End PBXCopyFilesBuildPhase section */")

# PBXFileReference
w("\n/* Begin PBXFileReference section */")
w(f'\t\t{PRODUCT_REF} /* Nyora.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = Nyora.app; sourceTree = BUILT_PRODUCTS_DIR; }};')
w(f'\t\t{W_PRODUCT_REF} /* NyoraWidgetsExtension.appex */ = {{isa = PBXFileReference; explicitFileType = "wrapper.app-extension"; includeInIndex = 0; path = NyoraWidgetsExtension.appex; sourceTree = BUILT_PRODUCTS_DIR; }};')
w(f'\t\t{INFO_PLIST_REF} /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; }};')
w(f'\t\t{ENTITLEMENTS_REF} /* Nyora.entitlements */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.entitlements; path = Nyora.entitlements; sourceTree = "<group>"; }};')
w(f'\t\t{W_INFO_REF} /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; name = Info.plist; path = "NyoraWidgets/Info.plist"; sourceTree = "<group>"; }};')
for rel in swift_files + resources:
    name = os.path.basename(rel)
    w(f'\t\t{file_refs[rel]} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = {ftype(rel)}; name = "{name}"; path = "NyoraApp/{rel}"; sourceTree = "<group>"; }};')
for rel in widget_files:
    w(f'\t\t{w_file_refs[rel]} /* {rel} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; name = "{rel}"; path = "NyoraWidgets/{rel}"; sourceTree = "<group>"; }};')
w("/* End PBXFileReference section */")

# PBXFrameworksBuildPhase
w("\n/* Begin PBXFrameworksBuildPhase section */")
w(f"\t\t{FW_PHASE} /* Frameworks */ = {{")
w("\t\t\tisa = PBXFrameworksBuildPhase;")
w("\t\t\tbuildActionMask = 2147483647;")
w("\t\t\tfiles = (")
w(f"\t\t\t\t{PKG_BUILDFILE} /* NyoraEngine in Frameworks */,")
w(f"\t\t\t\t{GOOGLE_BUILDFILE} /* GoogleSignIn in Frameworks */,")
w("\t\t\t);")
w("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
w("\t\t};")
w("/* End PBXFrameworksBuildPhase section */")

# PBXGroup
w("\n/* Begin PBXGroup section */")
w(f"\t\t{MAIN_GROUP} = {{")
w("\t\t\tisa = PBXGroup;")
w("\t\t\tchildren = (")
w(f"\t\t\t\t{APP_GROUP} /* NyoraApp */,")
w(f"\t\t\t\t{WIDGET_GROUP} /* NyoraWidgets */,")
w(f"\t\t\t\t{PRODUCTS_GROUP} /* Products */,")
w(f"\t\t\t\t{FRAMEWORKS_GROUP} /* Frameworks */,")
w("\t\t\t);")
w("\t\t\tsourceTree = \"<group>\";")
w("\t\t};")
w(f"\t\t{APP_GROUP} /* NyoraApp */ = {{")
w("\t\t\tisa = PBXGroup;")
w("\t\t\tchildren = (")
w(f"\t\t\t\t{INFO_PLIST_REF} /* Info.plist */,")
w(f"\t\t\t\t{ENTITLEMENTS_REF} /* Nyora.entitlements */,")
for rel in swift_files + resources:
    name = os.path.basename(rel)
    w(f"\t\t\t\t{file_refs[rel]} /* {name} */,")
w("\t\t\t);")
w("\t\t\tname = NyoraApp;")
w("\t\t\tsourceTree = \"<group>\";")
w("\t\t};")
w(f"\t\t{WIDGET_GROUP} /* NyoraWidgets */ = {{")
w("\t\t\tisa = PBXGroup;")
w("\t\t\tchildren = (")
w(f"\t\t\t\t{W_INFO_REF} /* Info.plist */,")
for rel in widget_files:
    w(f"\t\t\t\t{w_file_refs[rel]} /* {rel} */,")
w("\t\t\t);")
w("\t\t\tname = NyoraWidgets;")
w("\t\t\tsourceTree = \"<group>\";")
w("\t\t};")
w(f"\t\t{PRODUCTS_GROUP} /* Products */ = {{")
w("\t\t\tisa = PBXGroup;")
w("\t\t\tchildren = (")
w(f"\t\t\t\t{PRODUCT_REF} /* Nyora.app */,")
w(f"\t\t\t\t{W_PRODUCT_REF} /* NyoraWidgetsExtension.appex */,")
w("\t\t\t);")
w("\t\t\tname = Products;")
w("\t\t\tsourceTree = \"<group>\";")
w("\t\t};")
w(f"\t\t{FRAMEWORKS_GROUP} /* Frameworks */ = {{")
w("\t\t\tisa = PBXGroup;")
w("\t\t\tchildren = (")
w("\t\t\t);")
w("\t\t\tname = Frameworks;")
w("\t\t\tsourceTree = \"<group>\";")
w("\t\t};")
w("/* End PBXGroup section */")

# PBXNativeTarget
w("\n/* Begin PBXNativeTarget section */")
w(f"\t\t{TARGET} /* Nyora */ = {{")
w("\t\t\tisa = PBXNativeTarget;")
w(f"\t\t\tbuildConfigurationList = {TARGET_CFG_LIST} /* Build configuration list for PBXNativeTarget \"Nyora\" */;")
w("\t\t\tbuildPhases = (")
w(f"\t\t\t\t{SRC_PHASE} /* Sources */,")
w(f"\t\t\t\t{FW_PHASE} /* Frameworks */,")
w(f"\t\t\t\t{RES_PHASE} /* Resources */,")
w(f"\t\t\t\t{EMBED_PHASE} /* Embed Foundation Extensions */,")
w("\t\t\t);")
w("\t\t\tbuildRules = ();")
w("\t\t\tdependencies = (")
w(f"\t\t\t\t{W_DEP} /* PBXTargetDependency */,")
w("\t\t\t);")
w("\t\t\tname = Nyora;")
w("\t\t\tpackageProductDependencies = (")
w(f"\t\t\t\t{PKG_PRODUCT} /* NyoraEngine */,")
w(f"\t\t\t\t{GOOGLE_PKG_PRODUCT} /* GoogleSignIn */,")
w("\t\t\t);")
w("\t\t\tproductName = Nyora;")
w(f"\t\t\tproductReference = {PRODUCT_REF} /* Nyora.app */;")
w("\t\t\tproductType = \"com.apple.product-type.application\";")
w("\t\t};")
w(f"\t\t{W_TARGET} /* NyoraWidgetsExtension */ = {{")
w("\t\t\tisa = PBXNativeTarget;")
w(f"\t\t\tbuildConfigurationList = {W_CFG_LIST} /* Build configuration list for PBXNativeTarget \"NyoraWidgetsExtension\" */;")
w("\t\t\tbuildPhases = (")
w(f"\t\t\t\t{W_SRC_PHASE} /* Sources */,")
w("\t\t\t);")
w("\t\t\tbuildRules = ();")
w("\t\t\tdependencies = ();")
w("\t\t\tname = NyoraWidgetsExtension;")
w("\t\t\tproductName = NyoraWidgetsExtension;")
w(f"\t\t\tproductReference = {W_PRODUCT_REF} /* NyoraWidgetsExtension.appex */;")
w("\t\t\tproductType = \"com.apple.product-type.app-extension\";")
w("\t\t};")
w("/* End PBXNativeTarget section */")

# PBXProject
w("\n/* Begin PBXProject section */")
w(f"\t\t{PROJECT} /* Project object */ = {{")
w("\t\t\tisa = PBXProject;")
w("\t\t\tattributes = {")
w("\t\t\t\tBuildIndependentTargetsInParallel = 1;")
w("\t\t\t\tLastSwiftUpdateCheck = 1600;")
w("\t\t\t\tLastUpgradeCheck = 1600;")
w("\t\t\t\tTargetAttributes = {")
w(f"\t\t\t\t\t{W_TARGET} = {{ CreatedOnToolsVersion = 16.0; }};")
w("\t\t\t\t};")
w("\t\t\t};")
w(f"\t\t\tbuildConfigurationList = {PROJ_CFG_LIST} /* Build configuration list for PBXProject */;")
w("\t\t\tcompatibilityVersion = \"Xcode 14.0\";")
w("\t\t\tdevelopmentRegion = en;")
w("\t\t\thasScannedForEncodings = 0;")
w("\t\t\tknownRegions = ( en, Base );")
w(f"\t\t\tmainGroup = {MAIN_GROUP};")
w("\t\t\tpackageReferences = (")
w(f"\t\t\t\t{PKG_REF} /* XCLocalSwiftPackageReference \"NyoraEngine\" */,")
w(f"\t\t\t\t{GOOGLE_PKG_REF} /* XCRemoteSwiftPackageReference \"GoogleSignIn-iOS\" */,")
w("\t\t\t);")
w(f"\t\t\tproductRefGroup = {PRODUCTS_GROUP} /* Products */;")
w("\t\t\tprojectDirPath = \"\";")
w("\t\t\tprojectRoot = \"\";")
w("\t\t\ttargets = (")
w(f"\t\t\t\t{TARGET} /* Nyora */,")
w(f"\t\t\t\t{W_TARGET} /* NyoraWidgetsExtension */,")
w("\t\t\t);")
w("\t\t};")
w("/* End PBXProject section */")

# PBXResourcesBuildPhase
w("\n/* Begin PBXResourcesBuildPhase section */")
w(f"\t\t{RES_PHASE} /* Resources */ = {{")
w("\t\t\tisa = PBXResourcesBuildPhase;")
w("\t\t\tbuildActionMask = 2147483647;")
w("\t\t\tfiles = (")
for rel in resources:
    w(f"\t\t\t\t{build_files[rel]} /* {rel} in Resources */,")
w("\t\t\t);")
w("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
w("\t\t};")
w("/* End PBXResourcesBuildPhase section */")

# PBXSourcesBuildPhase
w("\n/* Begin PBXSourcesBuildPhase section */")
w(f"\t\t{SRC_PHASE} /* Sources */ = {{")
w("\t\t\tisa = PBXSourcesBuildPhase;")
w("\t\t\tbuildActionMask = 2147483647;")
w("\t\t\tfiles = (")
for rel in swift_files:
    w(f"\t\t\t\t{build_files[rel]} /* {rel} in Sources */,")
w("\t\t\t);")
w("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
w("\t\t};")
w(f"\t\t{W_SRC_PHASE} /* Sources */ = {{")
w("\t\t\tisa = PBXSourcesBuildPhase;")
w("\t\t\tbuildActionMask = 2147483647;")
w("\t\t\tfiles = (")
for rel in widget_files:
    w(f"\t\t\t\t{w_build_files[rel]} /* {rel} in Sources */,")
w("\t\t\t);")
w("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
w("\t\t};")
w("/* End PBXSourcesBuildPhase section */")

# PBXTargetDependency
w("\n/* Begin PBXTargetDependency section */")
w(f"\t\t{W_DEP} /* PBXTargetDependency */ = {{")
w("\t\t\tisa = PBXTargetDependency;")
w(f"\t\t\ttarget = {W_TARGET} /* NyoraWidgetsExtension */;")
w(f"\t\t\ttargetProxy = {W_PROXY} /* PBXContainerItemProxy */;")
w("\t\t};")
w("/* End PBXTargetDependency section */")

# XCBuildConfiguration
def proj_settings(debug):
    s = {
        "ALWAYS_SEARCH_USER_PATHS": "NO",
        "CLANG_ENABLE_MODULES": "YES",
        "CLANG_ENABLE_OBJC_ARC": "YES",
        "COPY_PHASE_STRIP": "NO",
        "ENABLE_STRICT_OBJC_MSGSEND": "YES",
        "GCC_C_LANGUAGE_STANDARD": "gnu11",
        "IPHONEOS_DEPLOYMENT_TARGET": "17.0",
        "SDKROOT": "iphoneos",
        "SWIFT_VERSION": "5.0",
    }
    if debug:
        s.update({
            "DEBUG_INFORMATION_FORMAT": "dwarf",
            "ENABLE_TESTABILITY": "YES",
            "GCC_OPTIMIZATION_LEVEL": "0",
            "ONLY_ACTIVE_ARCH": "YES",
            "SWIFT_OPTIMIZATION_LEVEL": "-Onone",
            "SWIFT_ACTIVE_COMPILATION_CONDITIONS": "DEBUG",
        })
    else:
        s.update({
            "DEBUG_INFORMATION_FORMAT": "\"dwarf-with-dsym\"",
            "SWIFT_OPTIMIZATION_LEVEL": "\"-Owholemodule\"",
            "VALIDATE_PRODUCT": "YES",
        })
    return s

def target_settings(debug):
    return {
        "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
        "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME": "AccentColor",
        "CODE_SIGNING_ALLOWED": "NO",
        "CODE_SIGN_STYLE": "Automatic",
        "CODE_SIGN_ENTITLEMENTS": "NyoraApp/Nyora.entitlements",
        "CURRENT_PROJECT_VERSION": "1",
        "GENERATE_INFOPLIST_FILE": "NO",
        "INFOPLIST_FILE": "NyoraApp/Info.plist",
        "INFOPLIST_KEY_UIApplicationSceneManifest_Generation": "YES",
        "INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents": "YES",
        "LD_RUNPATH_SEARCH_PATHS": "\"$(inherited) @executable_path/Frameworks\"",
        "MARKETING_VERSION": "1.0",
        "PRODUCT_BUNDLE_IDENTIFIER": "com.nyora.ios",
        "PRODUCT_NAME": "Nyora",
        "SWIFT_EMIT_LOC_STRINGS": "YES",
        "TARGETED_DEVICE_FAMILY": "\"1,2\"",
    }

def widget_settings(debug):
    return {
        "CODE_SIGNING_ALLOWED": "NO",
        "CODE_SIGN_STYLE": "Automatic",
        "CURRENT_PROJECT_VERSION": "1",
        "GENERATE_INFOPLIST_FILE": "NO",
        "INFOPLIST_FILE": "NyoraWidgets/Info.plist",
        "INFOPLIST_KEY_CFBundleDisplayName": "\"Nyora Widgets\"",
        "INFOPLIST_KEY_NSHumanReadableCopyright": "\"\"",
        "IPHONEOS_DEPLOYMENT_TARGET": "17.0",
        "LD_RUNPATH_SEARCH_PATHS": "\"$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks\"",
        "MARKETING_VERSION": "1.0",
        "PRODUCT_BUNDLE_IDENTIFIER": "com.nyora.ios.widgets",
        "PRODUCT_NAME": "\"$(TARGET_NAME)\"",
        "SKIP_INSTALL": "YES",
        "SWIFT_EMIT_LOC_STRINGS": "YES",
        "SWIFT_VERSION": "5.0",
        "TARGETED_DEVICE_FAMILY": "\"1,2\"",
    }

def emit_cfg(cid, name, settings):
    w(f"\t\t{cid} /* {name} */ = {{")
    w("\t\t\tisa = XCBuildConfiguration;")
    w("\t\t\tbuildSettings = {")
    for k in sorted(settings):
        w(f"\t\t\t\t{k} = {settings[k]};")
    w("\t\t\t};")
    w(f"\t\t\tname = {name};")
    w("\t\t};")

w("\n/* Begin XCBuildConfiguration section */")
emit_cfg(PROJ_DEBUG, "Debug", proj_settings(True))
emit_cfg(PROJ_RELEASE, "Release", proj_settings(False))
emit_cfg(TARGET_DEBUG, "Debug", target_settings(True))
emit_cfg(TARGET_RELEASE, "Release", target_settings(False))
emit_cfg(W_DEBUG, "Debug", widget_settings(True))
emit_cfg(W_RELEASE, "Release", widget_settings(False))
w("/* End XCBuildConfiguration section */")

# XCConfigurationList
w("\n/* Begin XCConfigurationList section */")
for cid, dbg, rel_, label in [
    (PROJ_CFG_LIST, PROJ_DEBUG, PROJ_RELEASE, "PBXProject"),
    (TARGET_CFG_LIST, TARGET_DEBUG, TARGET_RELEASE, "PBXNativeTarget \"Nyora\""),
    (W_CFG_LIST, W_DEBUG, W_RELEASE, "PBXNativeTarget \"NyoraWidgetsExtension\""),
]:
    w(f"\t\t{cid} /* Build configuration list for {label} */ = {{")
    w("\t\t\tisa = XCConfigurationList;")
    w("\t\t\tbuildConfigurations = (")
    w(f"\t\t\t\t{dbg} /* Debug */,")
    w(f"\t\t\t\t{rel_} /* Release */,")
    w("\t\t\t);")
    w("\t\t\tdefaultConfigurationIsVisible = 0;")
    w("\t\t\tdefaultConfigurationName = Release;")
    w("\t\t};")
w("/* End XCConfigurationList section */")

# XCLocalSwiftPackageReference
w("\n/* Begin XCLocalSwiftPackageReference section */")
w(f"\t\t{PKG_REF} /* XCLocalSwiftPackageReference \"NyoraEngine\" */ = {{")
w("\t\t\tisa = XCLocalSwiftPackageReference;")
w("\t\t\trelativePath = ../NyoraEngine;")
w("\t\t};")
w("/* End XCLocalSwiftPackageReference section */")

# XCRemoteSwiftPackageReference
w("\n/* Begin XCRemoteSwiftPackageReference section */")
w(f"\t\t{GOOGLE_PKG_REF} /* XCRemoteSwiftPackageReference \"GoogleSignIn-iOS\" */ = {{")
w("\t\t\tisa = XCRemoteSwiftPackageReference;")
w("\t\t\trepositoryURL = \"https://github.com/google/GoogleSignIn-iOS.git\";")
w("\t\t\trequirement = {")
w("\t\t\t\tkind = upToNextMajorVersion;")
w("\t\t\t\tminimumVersion = 7.1.0;")
w("\t\t\t};")
w("\t\t};")
w("/* End XCRemoteSwiftPackageReference section */")

# XCSwiftPackageProductDependency
w("\n/* Begin XCSwiftPackageProductDependency section */")
w(f"\t\t{PKG_PRODUCT} /* NyoraEngine */ = {{")
w("\t\t\tisa = XCSwiftPackageProductDependency;")
w("\t\t\tproductName = NyoraEngine;")
w("\t\t};")
w(f"\t\t{GOOGLE_PKG_PRODUCT} /* GoogleSignIn */ = {{")
w("\t\t\tisa = XCSwiftPackageProductDependency;")
w(f"\t\t\tpackage = {GOOGLE_PKG_REF} /* XCRemoteSwiftPackageReference \"GoogleSignIn-iOS\" */;")
w("\t\t\tproductName = GoogleSignIn;")
w("\t\t};")
w("/* End XCSwiftPackageProductDependency section */")

w("\t};")
w(f"\trootObject = {PROJECT} /* Project object */;")
w("}")

os.makedirs(PROJ, exist_ok=True)
with open(os.path.join(PROJ, "project.pbxproj"), "w") as f:
    f.write("\n".join(lines) + "\n")
print(f"Wrote {PROJ}/project.pbxproj with {len(swift_files)} app sources + {len(widget_files)} widget sources")
