# Maya Assistant — Skeleton

Personal Android voice-assistant skeleton: Accessibility Service +
GitHub Actions build (no PC needed).

## Kya hai isme

- `MainActivity.kt` — simple screen: ek button accessibility settings
  kholta hai, ek button voice sunta hai.
- `MayaAccessibilityService.kt` — screen ko read/control karne wala
  service (app open karna, button tap karna, text type karna).
- `.github/workflows/build.yml` — GitHub ka cloud server khud APK
  build karega, tumhe kuch install nahi karna.

## Phone se GitHub pe upload kaise karein

1. github.com pe naya **empty** repository banao (koi README/gitignore
   auto-add mat karo, warna clash hoga).
2. GitHub app ya browser (github.dev) se in sabhi files/folders ko
   waise hi upload karo jaise hain (folder structure maintain karo —
   `app/src/main/...` waisa hi rakhna hai).
3. Upload ho jane ke baad, repo ke **Actions** tab mein jao.
4. "Build Maya APK" workflow apne aap chalega (ya "Run workflow"
   button se manually bhi chala sakte ho).
5. Build complete hone ke baad, us run ke andar **Artifacts** section
   mein `maya-assistant-debug-apk` milega — download kar lo, phone
   pe install karo.

## Permission ka important point

Accessibility permission Android khud kabhi silently on nahi karta —
app install hone ke baad tumhe khud **Settings > Accessibility >
Maya Assistant** mein jaake switch ON karna padega. Ye safety feature
hai taaki koi app chupke se poora control na le sake.

## Aage kya jodna hai

- `MainActivity.kt` mein jahan `TODO` likha hai, wahan spoken text ko
  apne AI backend (jaise Claude API) ko bhejo.
- AI se jo action wapas aaye (e.g. "WhatsApp kholo", "message type
  karo"), use `MayaAccessibilityService.instance` ke through call
  karo — `openApp()`, `clickNodeByText()`, `typeIntoFocusedField()`
  already bane hue hain.

## Naya kya hai (Phase 2)

- **Notification/call sunna**: App khulne ke baad "Enable Notification
  Access" dabao (Settings mein khud ON karna padega). Naya notification
  aane par ya call aane par Maya bolke batayegi.
- **AI se command samajhna**: App mein apna Claude API key paste karke
  "Save API Key" dabao (console.anthropic.com se milega). Ab "Start
  Listening" se jo bhi bologe, Claude use samjhega aur action lega
  (app kholna, screen pe tap karna, text type karna, ya sirf jawab
  bolna).
- **Background mein chalna**: App khulte hi ek foreground service
  start ho jata hai (status bar mein "Maya is running" dikhega) —
  isse call/notification tab bhi track hote rahen jab app band ho.

### Zaroori: API key kabhi kisi ko mat bhejo
Ye key seedha tumhare phone ke local storage mein save hoti hai,
kahin upload nahi hoti. Isse bill tumhare Anthropic account pe aata
hai, isliye safe rakhna.

## Aage kya (Phase 3 — bahar se remote control)

Iske liye ek cloud relay chahiye (jaise Firebase Realtime Database):
phone ek "commands" folder sunega, aur tum kahin se bhi (web browser
se) us folder mein command likh doge. Iske liye pehle
firebase.google.com par free account/project banana padega — jab
ready ho, bata dena, uska integration code bana dunga.
