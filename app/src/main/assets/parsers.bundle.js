var NyoraParsers = (() => {
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __esm = (fn, res) => function __init() {
    return fn && (res = (0, fn[__getOwnPropNames(fn)[0]])(fn = 0)), res;
  };
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, { get: all[name], enumerable: true });
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

  // src/node_crypto_shim.js
  var node_crypto_shim_exports = {};
  __export(node_crypto_shim_exports, {
    createDecipheriv: () => createDecipheriv,
    default: () => node_crypto_shim_default,
    randomBytes: () => randomBytes
  });
  var randomBytes, createDecipheriv, node_crypto_shim_default;
  var init_node_crypto_shim = __esm({
    "src/node_crypto_shim.js"() {
      randomBytes = (n) => {
        const arr = new Uint8Array(n);
        (globalThis.crypto || self.crypto).getRandomValues(arr);
        return arr;
      };
      createDecipheriv = (algorithm, key, iv) => {
        throw new Error("node:crypto createDecipheriv not supported in WKWebView shim");
      };
      node_crypto_shim_default = { randomBytes, createDecipheriv };
    }
  });

  // src/ios_entry.js
  var ios_entry_exports = {};
  __export(ios_entry_exports, {
    SortOrder: () => SortOrder,
    getAllSources: () => getAllSources,
    getParser: () => getParser
  });

  // src/base.js
  var MangaState = {
    ONGOING: "ONGOING",
    FINISHED: "FINISHED",
    ABANDONED: "ABANDONED",
    PAUSED: "PAUSED",
    UPCOMING: "UPCOMING"
  };
  var SortOrder = {
    UPDATED: "UPDATED",
    UPDATED_ASC: "UPDATED_ASC",
    POPULARITY: "POPULARITY",
    POPULARITY_ASC: "POPULARITY_ASC",
    NEWEST: "NEWEST",
    NEWEST_ASC: "NEWEST_ASC",
    ALPHABETICAL: "ALPHABETICAL",
    ALPHABETICAL_DESC: "ALPHABETICAL_DESC",
    RATING: "RATING",
    RATING_ASC: "RATING_ASC",
    RELEVANCE: "RELEVANCE"
  };
  var ContentRating = {
    SAFE: "SAFE",
    ADULT: "ADULT"
  };
  function nyoraId(sourceName, url) {
    let h = 1125899906842597n;
    const s = String(sourceName == null ? "" : sourceName) + String(url == null ? "" : url);
    for (let i = 0; i < s.length; i++) {
      h = BigInt.asIntN(64, h * 31n + BigInt(s.charCodeAt(i)));
    }
    return BigInt.asIntN(64, h).toString();
  }
  var Manga = class {
    constructor(data) {
      this.id = data.id;
      this.url = data.url;
      this.publicUrl = data.publicUrl;
      this.coverUrl = data.coverUrl;
      this.largeCoverUrl = data.largeCoverUrl || data.coverUrl;
      this.title = data.title;
      this.altTitles = data.altTitles || [];
      this.rating = data.rating || 0;
      this.tags = data.tags || [];
      this.authors = data.authors || [];
      this.state = data.state;
      this.source = data.source;
      this.contentRating = data.contentRating;
      this.isNsfw = data.isNsfw || data.contentRating === ContentRating.ADULT;
      this.description = data.description || "";
      this.chapters = data.chapters || [];
    }
  };
  var MangaChapter = class {
    constructor(data) {
      this.id = data.id;
      this.url = data.url;
      this.title = data.title;
      this.number = data.number;
      this.volume = data.volume || 0;
      this.branch = data.branch;
      this.uploadDate = data.uploadDate || 0;
      this.scanlator = data.scanlator;
      this.source = data.source;
      this.pages = data.pages || [];
      this.index = data.index || 0;
    }
  };
  var MangaPage = class {
    constructor(data) {
      this.id = data.id;
      this.url = data.url;
      this.preview = data.preview;
      this.source = data.source;
      this.headers = data.headers || {};
    }
  };
  var BaseParser = class {
    constructor(context, source, domain, pageSize = 12) {
      this.context = context;
      this.source = source;
      this.domain = domain;
      this.pageSize = pageSize;
    }
    async getListPage(page, order, filter) {
      throw new Error("Not implemented");
    }
    async getDetails(manga) {
      throw new Error("Not implemented");
    }
    async getChapters(manga) {
      throw new Error("Not implemented");
    }
    async getPages(chapter) {
      throw new Error("Not implemented");
    }
    // Helpers
    toAbsoluteUrl(url) {
      if (!url)
        return "";
      if (url.startsWith("http"))
        return url;
      const base = this.domain.startsWith("http") ? this.domain : `https://${this.domain}`;
      return new URL(url, base).href;
    }
    toRelativeUrl(url) {
      if (!url)
        return "";
      if (!url.startsWith("http"))
        return url;
      try {
        const parsed = new URL(url);
        return parsed.pathname + parsed.search + parsed.hash;
      } catch {
        return url;
      }
    }
  };

  // src/madara.js
  var DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
  var MadaraParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 12) {
      super(context, source, domain, pageSize);
      this.withoutAjax = false;
      this.tagPrefix = "manga-genre/";
      this.datePattern = "MMMM d, yyyy";
      this.stylePage = "?style=list";
      this.postReq = false;
      this.ongoing = /* @__PURE__ */ new Set([
        "\u0645\u0633\u062A\u0645\u0631\u0629",
        "en curso",
        "ongoing",
        "on going",
        "OnGoing",
        "ativo",
        "en cours",
        "en cours \u{1F7E2}",
        "en cours de publication",
        "activo",
        "\u0111ang ti\u1EBFn h\xE0nh",
        "em lan\xE7amento",
        "\u043E\u043D\u0433\u043E\u0456\u043D\u0433",
        "publishing",
        "devam ediyor",
        "em andamento",
        "in corso",
        "g\xFCncel",
        "berjalan",
        "\u043F\u0440\u043E\u0434\u043E\u043B\u0436\u0430\u0435\u0442\u0441\u044F",
        "updating",
        "lan\xE7ando",
        "in arrivo",
        "emision",
        "en emision",
        "\u0645\u0633\u062A\u0645\u0631",
        "curso",
        "en marcha",
        "publicandose",
        "publicando",
        "\u8FDE\u8F7D\u4E2D"
      ]);
      this.finished = /* @__PURE__ */ new Set([
        "completed",
        "complete",
        "completo",
        "compl\xE9t\xE9",
        "fini",
        "achev\xE9",
        "termin\xE9",
        "termin\xE9 \u26AB",
        "tamamland\u0131",
        "\u0111\xE3 ho\xE0n th\xE0nh",
        "ho\xE0n th\xE0nh",
        "\u0645\u0643\u062A\u0645\u0644\u0629",
        "\u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043D\u043E",
        "\u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043D",
        "finished",
        "finalizado",
        "completata",
        "one-shot",
        "bitti",
        "tamat",
        "completado",
        "conclu\xEDdo",
        "concluido",
        "\u5DF2\u5B8C\u7ED3",
        "bitmi\u015F",
        "end",
        "\u0645\u0646\u062A\u0647\u064A\u0629"
      ]);
      this.abandoned = /* @__PURE__ */ new Set([
        "canceled",
        "cancelled",
        "cancelado",
        "cancellato",
        "cancelados",
        "dropped",
        "discontinued",
        "abandonn\xE9"
      ]);
      this.paused = /* @__PURE__ */ new Set([
        "hiatus",
        "on hold",
        "pausado",
        "en espera",
        "en pause",
        "en attente"
      ]);
      this.upcoming = /* @__PURE__ */ new Set([
        "upcoming",
        "\u0644\u0645 \u062A\u064F\u0646\u0634\u064E\u0631 \u0628\u0639\u062F",
        "prochainement",
        "\xE0 venir"
      ]);
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    isAsuraAstro() {
      return this.domain === "asurascans.com" || this.domain === "asuracomic.net";
    }
    asuraApiBase() {
      return "https://api.asurascans.com";
    }
    asuraCdnBase() {
      return "https://cdn.asurascans.com";
    }
    async getAsuraListPage(page, order, filter) {
      let url = `https://${this.domain}/browse?page=${page}`;
      if (filter.query)
        url += `&search=${encodeURIComponent(filter.query)}`;
      const html = await this.context.httpGet(url, { "User-Agent": DESKTOP_UA }, this);
      const doc = this.context.parseHTML(html);
      const seen = /* @__PURE__ */ new Set();
      const entries = [];
      for (const a of Array.from(doc.querySelectorAll('a[href*="/series/"], a[href*="/comics/"], a[href*="/manga/"]'))) {
        const href = a.getAttribute("href") || "";
        if (!href || href.includes("/chapter/"))
          continue;
        const relHref = this.toRelativeUrl(href).replace(/\/$/, "");
        if (seen.has(relHref))
          continue;
        const img = a.querySelector("img");
        const title = (img && img.getAttribute("alt") || a.textContent || "").trim();
        if (!title || title.length > 120)
          continue;
        seen.add(relHref);
        entries.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title,
          source: this.source,
          contentRating: ContentRating.SAFE
        }));
      }
      return entries;
    }
    asuraSeriesKey(url) {
      const rel = this.toRelativeUrl(url || "");
      const match = rel.match(/\/(series|comics|manga)\//);
      if (match) {
        const key = rel.substring(rel.indexOf(match[0]) + match[0].length).split(/[/?#]/)[0];
        return key || "";
      }
      return "";
    }
    async getAsuraDetails(manga) {
      const publicUrl = this.toAbsoluteUrl(manga.url).replace("/series/", "/comics/");
      let html = await this.context.httpGet(publicUrl, { "User-Agent": DESKTOP_UA }, this);
      let doc = this.context.parseHTML(html);
      const canonical = doc.querySelector('link[rel="canonical"]')?.getAttribute("href") || publicUrl;
      const key = this.asuraSeriesKey(canonical);
      let title = doc.querySelector("h1")?.textContent?.trim() || manga.title;
      let description = doc.querySelector('meta[name="description"]')?.getAttribute("content") || doc.querySelector('meta[property="og:description"]')?.getAttribute("content") || "";
      let chapters = Array.from(doc.querySelectorAll('a[href*="/chapter/"]')).map((a, i, all) => {
        const href = a.getAttribute("href");
        const relHref = this.toRelativeUrl(href).replace(/\/$/, "");
        const titleText = a.textContent.trim().replace(/\s+/g, " ");
        const numMatch = titleText.match(/Chapter\s+([\d.]+)/i);
        return new MangaChapter({
          id: relHref,
          url: relHref,
          title: titleText,
          number: numMatch ? parseFloat(numMatch[1]) : all.length - i,
          source: this.source
        });
      }).filter((c) => c.url.includes(key || ""));
      if (chapters.length === 0 || !description) {
        try {
          const apiBase = this.asuraApiBase();
          const text = await this.context.httpGet(`${apiBase}/api/series/${key}?nyoraTry=${Date.now()}`, { "User-Agent": DESKTOP_UA }, this);
          const res = JSON.parse(text);
          const series = res.series || res.data?.series || res.data || {};
          title = series.title || title;
          description = series.description || description;
          if (chapters.length === 0) {
            const cText = await this.context.httpGet(`${apiBase}/api/series/${key}/chapters?nyoraTry=${Date.now()}`, { "User-Agent": DESKTOP_UA }, this);
            const cRes = JSON.parse(cText);
            const rows = Array.isArray(cRes.data) ? cRes.data : [];
            chapters = rows.map((row) => new MangaChapter({
              id: `${canonical}/chapter/${row.number}`,
              url: `${canonical}/chapter/${row.number}`,
              title: row.title || `Chapter ${row.number}`,
              number: Number(row.number) || 0,
              source: this.source
            }));
          }
        } catch (e) {
        }
      }
      return new Manga({
        ...manga,
        id: canonical,
        url: canonical,
        publicUrl: canonical,
        title,
        description,
        source: this.source,
        chapters: chapters.sort((a, b) => b.number - a.number)
      });
    }
    async getAsuraPages(chapter) {
      const url = this.toAbsoluteUrl(chapter.url);
      const html = await this.context.httpGet(url, { "User-Agent": DESKTOP_UA }, this);
      const doc = this.context.parseHTML(html);
      let imageUrls = Array.from(doc.querySelectorAll("img[data-page-index], .reading-content img, .page-break img")).map((img) => this.imageSrc(img)).filter((src) => src && src.includes("asura-images"));
      if (!imageUrls.length) {
        imageUrls = Array.from(html.matchAll(/https:\/\/cdn\.asurascans\.com\/asura-images\/chapters\/[^"'<>\s)]+/g)).map((match) => match[0]);
      }
      if (imageUrls.length) {
        return imageUrls.map((url2, i) => new MangaPage({
          id: url2,
          url: url2,
          source: this.source
        }));
      }
      const key = this.asuraSeriesKey(chapter.url);
      const number = (this.toRelativeUrl(chapter.url).match(/\/chapter\/([^/?#]+)/) || [])[1];
      if (key && number) {
        try {
          const data = JSON.parse(await this.context.httpGet(`${this.asuraApiBase()}/api/series/${key}/chapters/${number}`, { "User-Agent": DESKTOP_UA }, this));
          const pages = data?.data?.chapter?.pages || [];
          return pages.map((page, i) => new MangaPage({
            id: page.url || String(i),
            url: page.url,
            source: this.source
          })).filter((p) => p.url);
        } catch (e) {
        }
      }
      return [];
    }
    parseChapterList(html) {
      const chapterDoc = this.context.parseHTML(html);
      const elements = this.queryAll(chapterDoc, [
        "li.wp-manga-chapter",
        "div.wp-manga-chapter",
        ".wp-manga-chapter",
        "ul.main.version-chap li",
        ".listing-chapters_wrap li",
        ".chapter-list li",
        ".chapters li"
      ]).reverse();
      return elements.map((el, i) => {
        const a = el.querySelector("a");
        if (!a)
          return null;
        const href = a.getAttribute("href");
        const relHref = this.toRelativeUrl(href);
        return new MangaChapter({
          id: relHref,
          url: relHref + this.stylePage,
          title: a.textContent.trim(),
          number: i + 1,
          source: this.source
        });
      }).filter((c) => c && c.url && !c.url.includes("#"));
    }
    async getListPage(page, order, filter) {
      if (this.isAsuraAstro()) {
        return this.getAsuraListPage(page, order, filter);
      }
      const domain = this.domain;
      if (this.withoutAjax) {
        const pages = page + 1;
        let url = `https://${domain}`;
        if (pages > 1)
          url += `/page/${pages}`;
        url += `/?s=${encodeURIComponent(filter.query || "")}&post_type=wp-manga`;
        let orderStr = "";
        switch (order) {
          case SortOrder.POPULARITY:
            orderStr = "views";
            break;
          case SortOrder.UPDATED:
            orderStr = "latest";
            break;
          case SortOrder.NEWEST:
            orderStr = "new-manga";
            break;
          case SortOrder.ALPHABETICAL:
            orderStr = "alphabet";
            break;
          case SortOrder.RATING:
            orderStr = "rating";
            break;
        }
        if (orderStr)
          url += `&m_orderby=${orderStr}`;
        const html = await this.context.httpGet(url, this);
        return this.parseMangaList(html);
      } else {
        const url = `https://${domain}/wp-admin/admin-ajax.php`;
        const params = new URLSearchParams();
        params.append("action", "madara_load_more");
        params.append("page", page.toString());
        params.append("template", "madara-core/content/content-search");
        params.append("vars[s]", filter.query || "");
        params.append("vars[post_type]", "wp-manga");
        params.append("vars[post_status]", "publish");
        params.append("vars[manga_archives_item_layout]", "default");
        switch (order) {
          case SortOrder.POPULARITY:
            params.append("vars[meta_key]", "_wp_manga_views");
            params.append("vars[orderby]", "meta_value_num");
            params.append("vars[order]", "desc");
            break;
          case SortOrder.UPDATED:
            params.append("vars[meta_key]", "_latest_update");
            params.append("vars[orderby]", "meta_value_num");
            params.append("vars[order]", "desc");
            break;
        }
        const html = await this.context.httpPost(url, params.toString(), {}, this);
        return this.parseMangaList(html);
      }
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const elements = doc.querySelectorAll("div.row.c-tabs-item__content, div.page-item-detail");
      const mangaList = [];
      for (const el of elements) {
        const a = el.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        const relHref = this.toRelativeUrl(href);
        const titleEl = el.querySelector("h3, h4, .manga-name, .post-title");
        const img = el.querySelector("img");
        mangaList.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title: titleEl ? titleEl.textContent.trim() : "",
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return mangaList;
    }
    async getDetails(manga) {
      if (this.isAsuraAstro()) {
        return this.getAsuraDetails(manga);
      }
      let html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const title = doc.querySelector("h1")?.textContent?.trim() || manga.title;
      const desc = doc.querySelector("div.description-summary div.summary__content, .post-content_item > h5 + div")?.innerHTML || "";
      const chapters = await this.loadChapters(manga.url, doc);
      return new Manga({
        ...manga,
        title,
        description: desc,
        chapters
      });
    }
    async loadChapters(mangaUrl, doc) {
      let chapterHtml;
      try {
        if (this.postReq) {
          const mangaId = doc.querySelector("div#manga-chapters-holder")?.getAttribute("data-id");
          if (mangaId) {
            const url = `https://${this.domain}/wp-admin/admin-ajax.php`;
            chapterHtml = await this.context.httpPost(url, `action=manga_get_chapters&manga=${mangaId}`, {
              "Content-Type": "application/x-www-form-urlencoded"
            }, this);
          }
        } else {
          const url = this.toAbsoluteUrl(mangaUrl).replace(/\/$/, "") + "/ajax/chapters/";
          chapterHtml = await this.context.httpPost(url, "", {}, this);
        }
      } catch {
        chapterHtml = "";
      }
      let chapters = chapterHtml ? this.parseChapterList(chapterHtml) : [];
      if (!chapters.length) {
        chapters = this.parseChapterList(doc.documentElement.outerHTML);
      }
      return chapters;
    }
    async getPages(chapter) {
      if (this.isAsuraAstro()) {
        return this.getAsuraPages(chapter);
      }
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      const images = doc.querySelectorAll("div.reading-content img, .page-break img");
      return Array.from(images).map((img) => {
        const imageUrl = this.imageSrc(img);
        return new MangaPage({
          id: imageUrl,
          url: imageUrl,
          source: this.source
        });
      });
    }
  };

  // src/mangareader.js
  var MangaReaderParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 12) {
      super(context, source, domain, pageSize);
      this.listUrl = "/manga";
      this.datePattern = "MMMM d, yyyy";
      this.selectMangaList = ".postbody .listupd .bs .bsx";
      this.selectMangaListImg = "img.ts-post-image";
      this.selectMangaListTitle = "div.tt";
      this.selectChapter = "#chapterlist > ul > li";
      this.encodedSrc = false;
      this.selectScript = "div.wrapper script";
      this.selectPage = "div#readerarea img";
      this.selectTestScript = "ts_reader";
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      return this.toImageUrl(url);
    }
    toImageUrl(url) {
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    chapterTitle(el, a) {
      const direct = el.querySelector(".chapternum, .chapter-title, .judulseries, .lchx")?.textContent?.trim();
      if (direct)
        return direct;
      const lines = (a?.textContent || el.textContent || "").split(/\n+/).map((line) => line.trim()).filter(Boolean);
      return lines[0] || "";
    }
    async getListPage(page, order, filter) {
      let url = `https://${this.domain}`;
      if (filter.query) {
        url += `/page/${page}/?s=${encodeURIComponent(filter.query)}`;
      } else {
        url += this.listUrl;
        url += "/?order=";
        switch (order) {
          case SortOrder.ALPHABETICAL:
            url += "title";
            break;
          case SortOrder.ALPHABETICAL_DESC:
            url += "titlereverse";
            break;
          case SortOrder.NEWEST:
            url += "latest";
            break;
          case SortOrder.POPULARITY:
            url += "popular";
            break;
          case SortOrder.UPDATED:
            url += "update";
            break;
        }
        if (filter.tags) {
          filter.tags.forEach((t) => url += `&genre[]=${encodeURIComponent(t.key)}`);
        }
        url += `&page=${page}`;
      }
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, [
        this.selectMangaList,
        ".postbody .listupd .bs .bsx",
        ".listupd .bs .bsx",
        ".listupd .bs",
        "div.animepost",
        "div.bge"
      ]);
      const mangaList = [];
      for (const el of elements) {
        const a = el.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        const relHref = this.toRelativeUrl(href);
        const titleEl = el.querySelector(this.selectMangaListTitle);
        const img = el.querySelector(this.selectMangaListImg) || el.querySelector("img");
        mangaList.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title: titleEl ? titleEl.textContent.trim() : (a.getAttribute("title") || a.textContent || "").trim(),
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return mangaList;
    }
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const title = doc.querySelector("h1.entry-title, .postbody h1")?.textContent?.trim() || manga.title;
      const desc = doc.querySelector("div.entry-content")?.innerHTML || "";
      const elements = this.queryAll(doc, [
        this.selectChapter,
        "#chapterlist > ul > li",
        "#chapterlist li",
        ".listing-chapters_wrap li",
        ".eplister li",
        ".episodelist li",
        ".bixbox.bxcl li",
        ".chapter-list li",
        "#Daftar_Chapter tr",
        "li.wp-manga-chapter",
        "div.wp-manga-chapter"
      ]).reverse();
      const chapters = elements.map((el, i) => {
        const a = el.querySelector("a");
        const href = a?.getAttribute("href");
        const relHref = href ? this.toRelativeUrl(href) : "";
        const title2 = this.chapterTitle(el, a);
        return new MangaChapter({
          id: relHref,
          url: relHref,
          title: title2,
          number: i + 1,
          source: this.source
        });
      }).filter((c) => c.url && !c.url.endsWith("/manga/") && !c.url.includes("#"));
      return new Manga({
        ...manga,
        title,
        description: desc,
        chapters
      });
    }
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      const hasTsReader = html.includes(this.selectTestScript);
      if (!hasTsReader && !this.encodedSrc) {
        const images = doc.querySelectorAll(this.selectPage);
        return Array.from(images).map((img) => {
          const imageUrl = this.toImageUrl(this.imageSrc(img));
          return new MangaPage({
            id: imageUrl,
            url: imageUrl,
            source: this.source
          });
        });
      } else {
        try {
          const match = html.match(/ts_reader\.run\((.*?)\);/s);
          if (match) {
            const data = JSON.parse(match[1]);
            const images = data.sources[0].images;
            return images.map((url) => {
              const imageUrl = this.toImageUrl(url);
              return new MangaPage({
                id: imageUrl,
                url: imageUrl,
                source: this.source
              });
            });
          }
        } catch (e) {
          console.error("Failed to parse ts_reader data", e);
        }
        return [];
      }
    }
  };

  // src/zeistmanga.js
  var ZeistMangaParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 12) {
      super(context, source, domain, pageSize);
      this.maxMangaResults = 20;
      this.mangaCategory = "Series";
      this.sateOngoing = "Ongoing";
      this.sateFinished = "Completed";
      this.sateAbandoned = "Cancelled";
      this.datePattern = "yyyy-MM-dd";
      this.selectTags = "article div.mt-15 a, .info-genre a, dl:contains(Genre) dd a";
      this.selectPage = "div.check-box img, article#reader .separator img, article.container .separator img, #readarea img, #reader img, #readerarea img";
      this.ongoing = /* @__PURE__ */ new Set([
        "ongoing",
        "en curso",
        "ativo",
        "lan\xE7ando",
        "lancando",
        "\u0645\u0633\u062A\u0645\u0631",
        "devam ediyor",
        "g\xFCncel",
        "guncel",
        "en emisi\xF3n",
        "en emision"
      ]);
      this.finished = /* @__PURE__ */ new Set([
        "completed",
        "completo",
        "tamamland\u0131",
        "tamamlandi",
        "finalizado",
        "finalizada"
      ]);
      this.abandoned = /* @__PURE__ */ new Set([
        "cancelled",
        "dropped",
        "dropado",
        "abandonado",
        "cancelado",
        "suspendido"
      ]);
      this.paused = /* @__PURE__ */ new Set([
        "hiatus"
      ]);
    }
    // queryAll fallback helper (mirrors madara.js). Tries each selector and
    // returns the first non-empty match, tolerating selectors a given DOM
    // implementation rejects.
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // Normalise a Blogger thumbnail url up to a larger render (=s###-c -> =w600).
    upgradeThumbnail(url) {
      if (!url)
        return "";
      return url.replace(/\/s.+?-c\//, "/w600/").replace(/=s(?!.*=s).+?-c$/, "=w600").replace(/\/s.+?-c-rw\//, "/w600/").replace(/=s(?!.*=s).+?-c-rw$/, "=w600");
    }
    // Pull the "alternate" link href out of a Blogger feed entry's link array.
    entryHref(entry) {
      const links = Array.isArray(entry.link) ? entry.link : [];
      const alt = links.find((l) => l && l.rel === "alternate") || links[0] || {};
      return alt.href || "";
    }
    // ----------------------------------------------------------------- LIST
    async getListPage(page, order, filter) {
      filter = filter || {};
      const startIndex = this.maxMangaResults * ((page || 1) - 1) + 1;
      const max = this.maxMangaResults + 1;
      let url = `https://${this.domain}/feeds/posts/default/-/`;
      if (filter.query) {
        url += `${encodeURIComponent(this.mangaCategory)}`;
        url += `?alt=json&orderby=published&max-results=${max}&start-index=${startIndex}`;
        url += `&q=label:${encodeURIComponent(this.mangaCategory)}+${encodeURIComponent(filter.query)}`;
      } else {
        const tags = filter.tags ? Array.from(filter.tags) : [];
        const states = filter.states ? Array.from(filter.states) : [];
        if (tags.length && states.length) {
          throw new Error("Filtering by both states and genres is not supported");
        }
        let label;
        if (tags.length) {
          const t = tags[0];
          label = t && (t.key || t) || this.mangaCategory;
        } else if (states.length) {
          switch (states[0]) {
            case MangaState.ONGOING:
              label = this.sateOngoing;
              break;
            case MangaState.FINISHED:
              label = this.sateFinished;
              break;
            case MangaState.ABANDONED:
              label = this.sateAbandoned;
              break;
            default:
              label = this.mangaCategory;
              break;
          }
        } else {
          label = this.mangaCategory;
        }
        url += `${encodeURIComponent(label)}`;
        url += `?alt=json&orderby=published&max-results=${max}&start-index=${startIndex}`;
      }
      const text = await this.context.httpGet(url, this);
      let feed;
      try {
        feed = JSON.parse(text).feed;
      } catch {
        return [];
      }
      if (!feed || !Array.isArray(feed.entry))
        return [];
      return this.parseMangaList(feed.entry);
    }
    parseMangaList(entries) {
      const out = [];
      for (const entry of entries) {
        const title = entry.title && entry.title.$t ? entry.title.$t : "";
        const href = this.entryHref(entry);
        if (!href)
          continue;
        let coverUrl = "";
        if (entry.media$thumbnail && entry.media$thumbnail.url) {
          coverUrl = this.upgradeThumbnail(entry.media$thumbnail.url);
        } else if (entry.content && entry.content.$t) {
          try {
            const cdoc = this.context.parseHTML(entry.content.$t);
            coverUrl = this.imageSrc(cdoc.querySelector("img"));
          } catch {
            coverUrl = "";
          }
        }
        const relHref = this.toRelativeUrl(href);
        out.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: href,
          coverUrl: coverUrl || "",
          title,
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return out;
    }
    // -------------------------------------------------------------- DETAILS
    mapState(text) {
      const t = (text || "").trim().toLowerCase();
      if (!t)
        return void 0;
      if (this.ongoing.has(t))
        return MangaState.ONGOING;
      if (this.finished.has(t))
        return MangaState.FINISHED;
      if (this.abandoned.has(t))
        return MangaState.ABANDONED;
      if (this.paused.has(t))
        return MangaState.PAUSED;
      return void 0;
    }
    // Replicates the Kotlin :contains() chain with plain DOM scanning.
    findByLabel(doc, containerSel, labels, valueSel) {
      const containers = this.queryAll(doc, [containerSel]);
      for (const c of containers) {
        const txt = (c.textContent || "").toLowerCase();
        if (labels.some((l) => txt.includes(l.toLowerCase()))) {
          const v = valueSel ? c.querySelector(valueSel) : c;
          if (v)
            return v;
        }
      }
      return null;
    }
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      let stateEl = this.findByLabel(doc, "div.y6x11p", ["Status", "Estado"], ".dt") || this.findByLabel(doc, "ul.infonime li", ["Status", "Estado"], "span") || doc.querySelector("span.status-novel") || doc.querySelector("span[data-status]") || doc.querySelector("[data-status]");
      const state = stateEl ? this.mapState(stateEl.textContent) : void 0;
      const authorEl = this.findByLabel(doc, "div.y6x11p", ["\u0627\u0644\u0643\u0627\u062A\u0628", "Author", "Autor", "Yazar"], ".dt") || this.findByLabel(doc, "dl", ["Author"], "dd") || this.findByLabel(doc, "ul.infonime li", ["Author"], "span");
      const authors = authorEl && authorEl.textContent.trim() ? [authorEl.textContent.trim()] : [];
      const descEl = doc.getElementById("synopsis") || doc.getElementById("Sinopse") || doc.getElementById("sinopas") || doc.querySelector(".sinopsis") || doc.querySelector(".sinopas");
      const description = descEl ? descEl.textContent.trim() : "";
      const tags = this.queryAll(doc, [this.selectTags]).map((a) => {
        const href = a.getAttribute("href") || "";
        const key = href.split("label/").pop().split("?")[0];
        return { key, title: a.textContent.trim() };
      }).filter((t) => t.key);
      const chapters = await this.loadChapters(manga.url, doc, html);
      return new Manga({
        ...manga,
        authors,
        tags,
        description,
        state,
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        chapters
      });
    }
    // Discover the chapter feed label across template variants, then fetch the
    // chapter feed JSON. Returns chapters oldest-first.
    async loadChapters(mangaUrl, doc, html) {
      let feed = null;
      const myUL = doc.getElementById("myUL");
      const latestScript = doc.querySelector("#latest > script");
      const clwdScript = doc.querySelector("#clwd > script");
      const chapterlist = doc.querySelector("#chapterlist");
      if (myUL) {
        const script = myUL.querySelector("script");
        const src = script ? script.getAttribute("src") || "" : "";
        if (src) {
          feed = decodeURIComponent(src.split("/-/").pop().split("?")[0]);
        }
      } else if (latestScript) {
        const m = (latestScript.textContent || "").match(/label\s*=\s*'([^']+)'/);
        if (m)
          feed = m[1];
      } else if (clwdScript) {
        const m = (clwdScript.textContent || "").match(/clwd\.run\('([^']+)'/);
        if (m)
          feed = m[1];
      } else if (chapterlist) {
        feed = chapterlist.getAttribute("data-post-title") || null;
      } else {
        const scripts = Array.from(doc.querySelectorAll("script"));
        const labelScript = scripts.find((s) => (s.textContent || "").includes("label_chapter"));
        const data = labelScript ? labelScript.textContent : html || "";
        const m = data.match(/label_chapter\s*=\s*"([^"]+)"/);
        if (m)
          feed = m[1];
      }
      if (!feed)
        return [];
      const url = `https://${this.domain}/feeds/posts/default/-/${feed}?alt=json&orderby=published&max-results=9999`;
      let entries;
      try {
        const json = JSON.parse(await this.context.httpGet(url, this));
        entries = json.feed && Array.isArray(json.feed.entry) ? json.feed.entry : [];
      } catch {
        return [];
      }
      const reversed = entries.slice().reverse();
      const slug = mangaUrl.split("/").filter(Boolean).pop();
      const chapters = [];
      let n = 0;
      for (const entry of reversed) {
        const title = entry.title && entry.title.$t ? entry.title.$t : "";
        const href = this.entryHref(entry);
        if (!href)
          continue;
        const slugChapter = href.split("/").filter(Boolean).pop();
        if (slug && slug === slugChapter)
          continue;
        const published = entry.published && entry.published.$t ? entry.published.$t : "";
        const dateText = published.split("T")[0];
        const ts = dateText ? Date.parse(dateText) : 0;
        n += 1;
        const relHref = this.toRelativeUrl(href);
        chapters.push(new MangaChapter({
          id: relHref,
          url: relHref,
          title,
          number: n,
          volume: 0,
          branch: null,
          uploadDate: Number.isFinite(ts) ? ts : 0,
          scanlator: null,
          source: this.source
        }));
      }
      return chapters;
    }
    // ----------------------------------------------------------------- PAGES
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      const scripts = Array.from(doc.querySelectorAll("script"));
      const chapterImageScript = scripts.find((s) => (s.textContent || "").includes("chapterImage ="));
      if (chapterImageScript) {
        const data = chapterImageScript.textContent;
        const inner = data.substring(data.indexOf("[") + 1, data.indexOf("]"));
        const urls = inner.replace(/\s/g, "").replace(/"/g, "").split(",").filter(Boolean);
        const pages = urls.map((url) => new MangaPage({
          id: url,
          url: this.toAbsoluteUrl(url),
          source: this.source
        })).filter((p) => p.url);
        if (pages.length)
          return pages;
      }
      const contentScript = scripts.find((s) => (s.textContent || "").includes("const content = "));
      if (contentScript) {
        const data = contentScript.textContent;
        const tickStart = data.indexOf("`");
        const tickEnd = data.indexOf("`;", tickStart + 1);
        if (tickStart >= 0 && tickEnd > tickStart) {
          const block = data.substring(tickStart + 1, tickEnd);
          const pages = block.split('src="').slice(1).map((seg) => {
            const url = seg.substring(0, seg.indexOf('"'));
            return new MangaPage({
              id: url,
              url: this.toAbsoluteUrl(url),
              source: this.source
            });
          }).filter((p) => p.url);
          if (pages.length)
            return pages;
        }
      }
      const imgs = this.queryAll(doc, [
        this.selectPage,
        "div.check-box img",
        "article#reader .separator img",
        "article.container .separator img",
        "#readarea img",
        "#reader img",
        "#readerarea img",
        "#reader div.separator img",
        ".post-body .separator img",
        ".entry-content img",
        "main .separator img",
        "main[data-chapters-id] img"
      ]);
      const fromDom = imgs.map((img) => {
        const url = this.imageSrc(img);
        return new MangaPage({ id: url, url, source: this.source });
      }).filter((p) => p.url && !p.url.startsWith("data:") && !p.url.startsWith("blob:"));
      if (fromDom.length)
        return fromDom;
      return this.extractImagesFromHtml(html);
    }
    extractImagesFromHtml(html) {
      let scope = html;
      const mainStart = html.search(/<main\b[^>]*data-chapters-id/i);
      if (mainStart >= 0) {
        const end = html.indexOf("</main>", mainStart);
        scope = end > mainStart ? html.substring(mainStart, end) : html.substring(mainStart);
      } else {
        const artStart = html.search(/<article\b/i);
        if (artStart >= 0) {
          const end = html.indexOf("</article>", artStart);
          if (end > artStart)
            scope = html.substring(artStart, end);
        }
      }
      const seen = /* @__PURE__ */ new Set();
      const pages = [];
      for (const m of scope.matchAll(/<img\b[^>]*?(?:data-src|src)\s*=\s*["']([^"']+)["']/gi)) {
        let url = m[1];
        if (!url || url.startsWith("data:") || url.startsWith("blob:"))
          continue;
        if (/(\/icon|avatar|emoji|=s\d{1,3}(-c)?$|\/s\d{1,3}(-c)?\/)/i.test(url))
          continue;
        url = this.toAbsoluteUrl(url);
        if (seen.has(url))
          continue;
        seen.add(url);
        pages.push(new MangaPage({ id: url, url, source: this.source }));
      }
      return pages;
    }
  };

  // src/onemanga.js
  var OneMangaParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 1) {
      super(context, source, domain, pageSize);
      this.selectTitle = "ul.elementor-nav-menu li a";
      this.selectCover = "div.elementor-widget-container img";
      this.selectInfoList = "div.elementor-widget-text-editor ul li";
      this.authorLabel = "Auteur(s)";
      this.altTitleLabel = "Nom(s) Alternatif(s)";
      this.selectChaptersHolder = "#All_chapters";
      this.selectChapterLink = "ul li a";
      this.selectPage = "div.elementor-widget-container img";
    }
    queryAll(doc, selectors) {
      for (const selector of (selectors || []).filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    findInfoValue(doc, label) {
      for (const li of this.queryAll(doc, [this.selectInfoList, "div.elementor-widget-text-editor ul li"])) {
        const text = (li.textContent || "").trim();
        if (text.toLowerCase().includes(label.toLowerCase())) {
          return text.replace(new RegExp(`^${label.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}\\s*:?\\s*`, "i"), "").trim();
        }
      }
      return "";
    }
    // The whole site is one manga; the homepage URL is its canonical id/url.
    async getListPage(page, order, filter) {
      if (page && page > 1)
        return [];
      const url = `https://${this.domain}`;
      const html = await this.context.httpGet(url, this);
      const doc = this.context.parseHTML(html);
      const title = (this.queryAll(doc, [this.selectTitle])[0]?.textContent || "").trim();
      const coverImg = this.queryAll(doc, [this.selectCover])[0] || null;
      const author = this.findInfoValue(doc, this.authorLabel);
      const altTitle = this.findInfoValue(doc, this.altTitleLabel);
      const infoItems = this.queryAll(doc, [this.selectInfoList]);
      const description = infoItems.length ? (infoItems[infoItems.length - 1].textContent || "").trim() : "";
      const relUrl = this.toRelativeUrl(url) || "/";
      if (!title && !this.queryAll(doc, [this.selectChaptersHolder]).length) {
        return [];
      }
      return [new Manga({
        id: relUrl,
        url: relUrl,
        publicUrl: url,
        coverUrl: this.imageSrc(coverImg),
        title: title || this.source.title || this.domain,
        altTitles: altTitle ? [altTitle] : [],
        authors: author ? [author] : [],
        tags: [],
        description,
        state: null,
        source: this.source,
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
      })];
    }
    async getDetails(manga) {
      const fullUrl = this.toAbsoluteUrl(manga.url || `https://${this.domain}`);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      const holder = this.queryAll(doc, [this.selectChaptersHolder, "#All_chapters", "#all_chapters"])[0] || doc;
      let links = this.queryAll(holder, [this.selectChapterLink, "ul li a", "li a", "a"]);
      links = links.reverse();
      const chapters = links.map((a, i) => {
        const href = a.getAttribute("href") || "";
        const relHref = this.toRelativeUrl(href);
        return new MangaChapter({
          id: relHref,
          url: relHref,
          title: (a.textContent || "").trim(),
          number: i + 1,
          volume: 0,
          branch: null,
          scanlator: null,
          uploadDate: 0,
          source: this.source
        });
      }).filter((c) => c.url && !c.url.startsWith("#") && !c.url.startsWith("javascript:"));
      return new Manga({ ...manga, chapters });
    }
    async getPages(chapter) {
      const fullUrl = this.toAbsoluteUrl(chapter.url);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      const images = this.queryAll(doc, [this.selectPage, "div.elementor-widget-container img"]);
      return images.map((img) => {
        const url = this.imageSrc(img);
        return new MangaPage({
          id: url,
          url,
          preview: null,
          source: this.source
        });
      }).filter((p) => p.url && !p.url.startsWith("data:") && !p.url.startsWith("blob:"));
    }
  };

  // src/hotcomics.js
  var HotComicsParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 24) {
      super(context, source, domain, pageSize);
      const isTooMics = /toomics\./i.test(domain);
      this.mangasUrl = isTooMics ? "/webtoon/ranking/genre" : "/genres";
      this.onePage = isTooMics;
      this.isSearchSupported = !isTooMics;
      this.selectMangas = isTooMics ? "li > div.visual" : "li[itemtype*=ComicSeries]:not(.no-comic)";
      this.selectMangaChapters = isTooMics ? "li.normal_ep:has(.coin-type1)" : "#tab-chapter li";
      this.selectTagsList = isTooMics ? "div.genre_list li:not(.on) a" : ".genres-list li:not(.on) a";
      this.selectPages = isTooMics ? "div[id^=load_image_] img" : "#viewer-img img";
      this.usePopupLoginChapters = /hotcomics\.|daycomics\./i.test(domain);
      this.datePattern = "MMM dd, yyyy";
    }
    // -------- generic helpers (mirrors madara.js conventions) --------
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    queryFirst(el, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const found = el.querySelector(selector);
          if (found)
            return found;
        } catch {
        }
      }
      return null;
    }
    // TooMics gates its detail/reader pages on the Referer header: a Referer of
    // the site ROOT (https://host/<lang>/) makes /webtoon/episode/toon/<id>
    // 302-redirect to the first /ep/1 reader page, while a Referer that matches
    // the page's own path keeps it on the full chapter-list page. Nyora sends
    // Referer = mangaUrl for exactly this reason.
    //
    // Neither the smoke harness nor the production runtime let a parser pass
    // per-request headers; both derive the Referer from `this.domain`
    // (https://${this.domain}/). So we briefly point `this.domain` at the page's
    // own path, issue the request, then restore it. We also retry with a
    // cache-buster because the gate is occasionally flaky.
    async httpGetAs(url, refererDomain, tries = 3) {
      const saved = this.domain;
      if (refererDomain)
        this.domain = refererDomain;
      try {
        let lastErr;
        for (let i = 0; i < tries; i++) {
          const target = i === 0 ? url : `${url}${url.includes("?") ? "&" : "?"}nyoraTry=${Date.now()}-${i}`;
          try {
            return await this.context.httpGet(target, this);
          } catch (e) {
            lastErr = e;
          }
        }
        throw lastErr;
      } finally {
        this.domain = saved;
      }
    }
    async httpGetRetry(url, tries = 3) {
      return this.httpGetAs(url, null, tries);
    }
    // Build the "domain" string (host + path, no scheme) used to drive a Referer
    // equal to a given absolute/relative page URL.
    refererDomainFor(pageUrl) {
      const abs = this.absFromHost(pageUrl);
      return abs.replace(/^https?:\/\//, "").replace(/\/+$/, "");
    }
    // Element.src() port: try lazy-load attrs then src, resolve to absolute.
    imageSrc(img) {
      if (!img)
        return "";
      const names = [
        "data-src",
        "data-cfsrc",
        "data-original",
        "data-cdn",
        "data-lazy-src",
        "original-src",
        "data-wpfc-original-src",
        "src"
      ];
      for (const name of names) {
        const v = img.getAttribute(name);
        if (v && !v.startsWith("data:") && !v.startsWith("blob:")) {
          return this.toAbsoluteUrl(v.trim());
        }
      }
      return "";
    }
    // The bare host of this.domain, without the locale path segment.
    hostBase() {
      const d = this.domain.startsWith("http") ? this.domain : `https://${this.domain}`;
      try {
        return `https://${new URL(d).hostname}`;
      } catch {
        return `https://${this.domain.split("/")[0]}`;
      }
    }
    // Resolve an extracted href to a relative URL with its leading locale
    // segment stripped, matching the Kotlin substringAfter('/') logic.
    stripLocale(href) {
      if (!href)
        return "";
      let rel;
      if (href.startsWith("http")) {
        try {
          const u = new URL(href);
          rel = u.pathname + u.search;
        } catch {
          rel = href;
        }
      } else {
        rel = href;
      }
      if (rel.startsWith("/")) {
        const trimmed = rel.replace(/^\/+/, "");
        const idx = trimmed.indexOf("/");
        rel = "/" + (idx >= 0 ? trimmed.slice(idx + 1) : "");
      }
      return rel;
    }
    // Absolute URL built like Nyora's String.toAbsoluteUrl(domain): the
    // (locale-stripped) relative URL is concatenated onto the host + the
    // normalized locale segment (e.g. "toomics.com" + "/it"). Plain
    // new URL(rel, host) would drop the locale, so we concat by hand.
    absFromHost(relUrl) {
      if (!relUrl)
        return "";
      if (relUrl.startsWith("//"))
        return `https:${relUrl}`;
      if (relUrl.startsWith("http"))
        return relUrl;
      const base = `${this.hostBase()}${this.localeSegment()}`.replace(/\/+$/, "");
      if (relUrl.startsWith("/"))
        return `${base}${relUrl}`;
      return `${base}/${relUrl}`;
    }
    // -------- list --------
    async getListPage(page, order, filter) {
      filter = filter || {};
      if (this.onePage && page > 1)
        return [];
      const tags = (filter.tags || []).map((t) => typeof t === "string" ? t : t.key || t.title).filter(Boolean);
      let url = `${this.hostBase()}`;
      const localeSeg = this.localeSegment();
      if (filter.query && this.isSearchSupported) {
        url += `${localeSeg}/search?keyword=${encodeURIComponent(filter.query)}&page=${page}`;
      } else {
        url += `${localeSeg}${this.mangasUrl}`;
        if (tags.length)
          url += `/${tags[0]}`;
        if (!this.onePage)
          url += `?page=${page}`;
      }
      const refererDomain = url.replace(/^https?:\/\//, "").replace(/\/+$/, "");
      const html = await this.httpGetAs(url, refererDomain);
      const doc = this.context.parseHTML(html);
      return this.parseMangaList(doc);
    }
    // The locale path segment from this.domain (e.g. "/ja", "/en", "/por").
    // TooMics serves Spanish only under "/esp"; the legacy "/es" and "/mx"
    // segments 302-redirect there but require a content_lang cookie that a
    // stateless fetch cannot satisfy mid-redirect, so we normalize up front.
    localeSegment() {
      const d = this.domain.replace(/^https?:\/\//, "");
      const idx = d.indexOf("/");
      let seg = idx >= 0 ? d.slice(idx) : "";
      if (/toomics\./i.test(this.domain) && (seg === "/es" || seg === "/mx")) {
        seg = "/esp";
      }
      return seg;
    }
    parseMangaList(doc) {
      const isFinished = !!this.queryFirst(doc, [".ico_fin"]);
      const items = this.queryAll(doc, [
        this.selectMangas,
        "li[itemtype*=ComicSeries]:not(.no-comic)",
        "li > div.visual",
        "li[itemtype*=ComicSeries]"
      ]);
      const list = [];
      for (const li of items) {
        const a = li.querySelector("a") || li.closest && li.closest("a");
        if (!a)
          continue;
        const href = a.getAttribute("href") || "";
        if (!href || href.startsWith("javascript") || href === "#")
          continue;
        const rel = this.stripLocale(href);
        if (!rel || rel === "/")
          continue;
        const img = li.querySelector("img");
        const titleEl = this.queryFirst(li, [".title", ".subject", "strong.title", "h3", "h4"]);
        const title = (titleEl ? titleEl.textContent : img && img.getAttribute("alt") || "").trim();
        if (!title)
          continue;
        const isNsfwCard = !!this.queryFirst(a, [".ico-18plus"]) || !!this.queryFirst(li, [".ico-18plus"]);
        const descEl = this.queryFirst(li, ["p[itemprop*=description]", "p.desc", ".summary"]);
        const author = (this.queryFirst(li, [".writer", ".author"]) || {}).textContent || "";
        list.push(new Manga({
          id: rel,
          url: rel,
          publicUrl: this.absFromHost(rel),
          coverUrl: this.imageSrc(img),
          title,
          description: descEl ? descEl.textContent.trim() : "",
          authors: author.trim() ? [author.trim()] : [],
          state: isFinished ? MangaState.FINISHED : MangaState.ONGOING,
          source: this.source,
          contentRating: isNsfwCard || this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return list;
    }
    // -------- details --------
    async getDetails(manga) {
      const mangaUrl = this.absFromHost(manga.url);
      const html = await this.httpGetAs(mangaUrl, this.refererDomainFor(manga.url));
      const doc = this.context.parseHTML(html);
      const descEl = this.queryFirst(doc, [
        "div.title_content_box h2",
        ".title_content_box h2",
        ".synopsis",
        'meta[property="og:title"]'
      ]);
      let description = "";
      if (descEl) {
        description = descEl.tagName === "META" ? descEl.getAttribute("content") || "" : descEl.textContent.trim();
      }
      description = description || manga.description;
      let chapters = this.usePopupLoginChapters ? this.parsePopupLoginChapters(doc) : this.parseChapterList(doc);
      if (!chapters.length) {
        chapters = this.parseReaderChapters(doc, mangaUrl);
      }
      return new Manga({
        ...manga,
        description,
        contentRating: manga.contentRating || (this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE),
        source: this.source,
        chapters
      });
    }
    // TooMics reader-page fallback: the /webtoon/detail/code/<c>/ep/<n>/toon/<id>
    // page exposes the current episode plus next/prev episode links. Collect
    // every distinct /ep/N/toon/ reader URL we can see, including the current
    // page (extracted from og:url / canonical), so getPages always has a target.
    parseReaderChapters(doc, landedUrl) {
      const seen = /* @__PURE__ */ new Map();
      const consider = (raw) => {
        if (!raw)
          return;
        const rel = this.stripLocale(raw);
        const m = rel.match(/\/ep\/(\d+)\/toon\//);
        if (!m)
          return;
        if (!seen.has(rel))
          seen.set(rel, parseInt(m[1], 10));
      };
      const canonical = doc.querySelector('link[rel="canonical"]')?.getAttribute("href") || doc.querySelector('meta[property="og:url"]')?.getAttribute("content") || landedUrl;
      consider(canonical);
      for (const a of doc.querySelectorAll("a")) {
        const href = a.getAttribute("href") || "";
        const onclick = a.getAttribute("onclick") || "";
        consider(href);
        const m = onclick.match(/location\.href='([^']+)'/);
        if (m)
          consider(m[1]);
      }
      const entries = Array.from(seen.entries()).sort((a, b) => a[1] - b[1]);
      return entries.map(([rel, num], i) => new MangaChapter({
        id: rel,
        url: rel,
        title: `Ep. ${num}`,
        number: Number.isFinite(num) ? num : i + 1,
        volume: 0,
        uploadDate: 0,
        source: this.source
      }));
    }
    // Default (TooMics) chapter list: anchors inside selectMangaChapters items.
    parseChapterList(doc) {
      const items = this.queryAll(doc, [
        this.selectMangaChapters,
        "#tab-chapter li",
        "li.normal_ep:has(.coin-type1)",
        "li.normal_ep",
        "#tab-chapter a"
      ]);
      const chapters = [];
      items.forEach((li, i) => {
        const a = li.tagName === "A" ? li : li.querySelector("a");
        if (!a)
          return;
        const rel = this.chapterHrefFromAnchor(a);
        if (!rel || rel === "/")
          return;
        const numEl = this.queryFirst(li, [".num"]);
        const num = numEl ? parseFloat(numEl.textContent.trim()) : NaN;
        const timeEl = this.queryFirst(li, ["time"]);
        const dateText = timeEl ? timeEl.getAttribute("datetime") || timeEl.textContent : "";
        chapters.push(new MangaChapter({
          id: rel,
          url: rel,
          title: null,
          number: Number.isFinite(num) ? num : i + 1,
          volume: 0,
          uploadDate: this.parseDate(dateText),
          source: this.source
        }));
      });
      return chapters;
    }
    // hotcomics.me / daycomics.me: chapter URL lives in onclick=popupLogin('url').
    parsePopupLoginChapters(doc) {
      const anchors = this.queryAll(doc, ["#tab-chapter a", "#tab-chapter li a"]);
      const chapters = [];
      anchors.forEach((a, i) => {
        const onclick = a.getAttribute("onclick") || "";
        let raw = "";
        const m = onclick.match(/popupLogin\('([^']+)'/);
        if (m)
          raw = m[1];
        else
          raw = a.getAttribute("href") || "";
        const rel = this.stripLocale(raw);
        if (!rel || rel === "/")
          return;
        const nameEl = this.queryFirst(a, [".cell-num"]);
        const name = nameEl ? nameEl.textContent.trim() : "Unknown";
        const timeEl = this.queryFirst(a, [".cell-time", "time"]);
        const dateText = timeEl ? timeEl.getAttribute("datetime") || timeEl.textContent : "";
        const numEl = this.queryFirst(a, [".num"]);
        const num = numEl ? parseFloat(numEl.textContent.trim()) : NaN;
        chapters.push(new MangaChapter({
          id: rel,
          url: rel,
          title: name,
          number: Number.isFinite(num) ? num : i + 1,
          volume: 0,
          uploadDate: this.parseDate(dateText),
          source: this.source
        }));
      });
      return chapters;
    }
    // Extract a chapter href from an anchor, handling javascript:/onclick links.
    chapterHrefFromAnchor(a) {
      let href = a.getAttribute("href") || "";
      if (href.startsWith("javascript") || !href || href === "#") {
        const onclick = a.getAttribute("onclick") || "";
        const m = onclick.match(/href='([^']+)'/) || onclick.match(/popupLogin\('([^']+)'/);
        if (m)
          href = m[1];
      }
      return this.stripLocale(href);
    }
    parseDate(text) {
      if (!text)
        return 0;
      const t = String(text).trim();
      const parsed = Date.parse(t);
      return Number.isNaN(parsed) ? 0 : parsed;
    }
    // -------- pages --------
    async getPages(chapter) {
      const fullUrl = this.absFromHost(chapter.url);
      const html = await this.httpGetRetry(fullUrl);
      const doc = this.context.parseHTML(html);
      const imgs = this.queryAll(doc, [
        this.selectPages,
        "div[id^=load_image_] img",
        "#viewer-img img",
        "#viewer img",
        ".viewer img"
      ]);
      return imgs.map((img, i) => {
        const url = this.imageSrc(img);
        return new MangaPage({
          id: url || String(i),
          url,
          source: this.source
        });
      }).filter((p) => p.url);
    }
  };

  // src/wpcomics.js
  var WpComicsParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 48) {
      super(context, source, domain, pageSize);
      this.listUrl = "/tim-truyen";
      this.datePattern = "dd/MM/yy";
      this.coverDiv = "div.image a img";
      this.selectDesc = "div.detail-content p";
      this.selectState = "div.col-info li.status p:not(.name)";
      this.selectAut = "div.col-info li.author p:not(.name), li.author p.col-xs-8";
      this.selectTag = "div.col-info li.kind p:not(.name) a, li.kind p.col-xs-8 a";
      this.selectDate = "div.col-xs-4";
      this.selectChapter = "div.list-chapter li.row:not(.heading)";
      this.selectPage = "div.page-chapter > img, li.blocks-gallery-item img, div.page-chapter img";
      this.ongoing = /* @__PURE__ */ new Set([
        "\u0111ang ti\u1EBFn h\xE0nh",
        "\u0111ang c\u1EADp nh\u1EADt",
        "ongoing",
        "updating",
        "\u9023\u8F09\u4E2D"
      ]);
      this.finished = /* @__PURE__ */ new Set([
        "ho\xE0n th\xE0nh",
        "\u0111\xE3 ho\xE0n th\xE0nh",
        "complete",
        "completed",
        "\u5B8C\u7D50\u6E08\u307F"
      ]);
      this.adFragments = [
        "sp1.jpg",
        "sp2.jpg",
        "3q_fake",
        "3qui5.jpg",
        "3qui6.jpg",
        "3qui8.jpg",
        "3qui9.jpg",
        "3qui10.jpg",
        "3qui12.jpg",
        "3qui13.jpg",
        "3q_top",
        "3q282.jpg",
        "3qui5_banner.jpg",
        "dt3qui8.jpg",
        "toptruyentv.jpg",
        "follow.png",
        "image_default.png",
        "toptruyentv5.jpg",
        "toptruyentv6.jpg",
        "toptruyentv7.jpg",
        "toptruyentv8.jpg",
        "toptruyentv9.jpg",
        "img_001_1743221470.png"
      ];
    }
    // ---- helpers (mirrors madara.js conventions) -------------------------
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    queryFirst(root, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const el = root.querySelector(selector);
          if (el)
            return el;
        } catch {
        }
      }
      return null;
    }
    /**
     * WpComics lazy-loads covers; the real URL can live in any attribute that
     * parses as an http(s) URL (data-original / data-src / src). `src` has the
     * lowest priority (mirrors Kotlin's findImageUrl()).
     */
    imageSrc(img) {
      if (!img)
        return "";
      const candidates = [
        img.getAttribute("data-original"),
        img.getAttribute("data-src"),
        img.getAttribute("data-lazy-src"),
        img.getAttribute("src")
      ];
      for (const c of candidates) {
        if (!c)
          continue;
        if (c.startsWith("data:") || c.startsWith("blob:"))
          continue;
        return this.toAbsoluteUrl(c);
      }
      return "";
    }
    contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    isItemMangaSite() {
      const id = this.source && this.source.id || "";
      return id === "DOCTRUYEN3Q" || id === "TOPTRUYEN" || /doctruyen3q|toptruyen/i.test(this.domain);
    }
    isMangaRaw() {
      const id = this.source && this.source.id || "";
      return id === "MANGARAW" || /mangaraw\.best/i.test(this.domain);
    }
    parseRating(doc) {
      const input = this.queryFirst(doc, ["div.star input", "div.star input[value]"]);
      const v = input && parseFloat(input.getAttribute("value"));
      return v && !Number.isNaN(v) ? v / 5 : 0;
    }
    /** Relative VI dates ("3 giờ trước") and absolute dd-MM-yyyy / dd/MM/yy. */
    parseChapterDate(text) {
      if (!text)
        return 0;
      const d = text.toLowerCase().trim();
      const now = Date.now();
      const num = (() => {
        const m2 = d.match(/(\d+)/);
        return m2 ? parseInt(m2[1], 10) : 0;
      })();
      if (/giây|second/.test(d) && /trước|ago/.test(d))
        return now - num * 1e3;
      if (/phút|min/.test(d) && /trước|ago/.test(d))
        return now - num * 60 * 1e3;
      if (/(giờ|hour|\bh\b)/.test(d) && /trước|ago/.test(d))
        return now - num * 3600 * 1e3;
      if (/ngày|day/.test(d) && /trước|ago/.test(d))
        return now - num * 86400 * 1e3;
      if (/tuần|week/.test(d) && /trước|ago/.test(d))
        return now - num * 7 * 86400 * 1e3;
      if (/tháng|month/.test(d) && /trước|ago/.test(d))
        return now - num * 30 * 86400 * 1e3;
      if (/năm|year/.test(d) && /trước|ago/.test(d))
        return now - num * 365 * 86400 * 1e3;
      const m = d.match(/(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})/);
      if (m) {
        let [, dd, mm, yy] = m;
        let year = parseInt(yy, 10);
        if (year < 100)
          year += 2e3;
        const t = Date.UTC(year, parseInt(mm, 10) - 1, parseInt(dd, 10));
        if (!Number.isNaN(t))
          return t;
      }
      return 0;
    }
    // ---- list ------------------------------------------------------------
    buildListUrl(page, order, filter) {
      const id = this.source && this.source.id || "";
      const q = filter && filter.query ? encodeURIComponent(filter.query) : "";
      if (id === "NETTRUYENHE") {
        if (q)
          return `https://${this.domain}/search/${page}/?keyword=${q}`;
        const tag2 = filter && filter.tags && filter.tags[0];
        let sort2 = "latest-updated";
        switch (order) {
          case SortOrder.POPULARITY:
            sort2 = "views";
            break;
          case SortOrder.NEWEST:
            sort2 = "new";
            break;
          case SortOrder.RATING:
            sort2 = "score";
            break;
          case SortOrder.ALPHABETICAL:
            sort2 = "az";
            break;
          case SortOrder.ALPHABETICAL_DESC:
            sort2 = "za";
            break;
          default:
            sort2 = "latest-updated";
        }
        return `https://${this.domain}${this.listUrl}/${page}/?genres=${tag2 ? tag2.key : ""}&notGenres=&sex=All&chapter_count=0&sort=${sort2}`;
      }
      if (id === "XOXOCOMICS" || /xoxocomic/i.test(this.domain)) {
        if (q)
          return `https://${this.domain}/search-comic?keyword=${q}&page=${page}`;
        let seg = "/comic-update";
        switch (order) {
          case SortOrder.POPULARITY:
            seg = "/popular-comic";
            break;
          case SortOrder.NEWEST:
            seg = "/new-comic";
            break;
          case SortOrder.ALPHABETICAL:
            seg = this.listUrl;
            break;
          default:
            seg = "/comic-update";
        }
        return `https://${this.domain}${seg}?page=${page}`;
      }
      if (this.isItemMangaSite()) {
        let url2 = `https://${this.domain}/tim-truyen`;
        const tag2 = filter && filter.tags && filter.tags[0];
        if (tag2)
          url2 += `/${tag2.key}`;
        const params = [];
        if (order === SortOrder.UPDATED)
          params.push("sort=1");
        else if (order === SortOrder.POPULARITY)
          params.push("sort=2");
        if (q)
          params.push(`keyword=${q}`);
        if (page > 1)
          params.push(`page=${page}`);
        if (params.length)
          url2 += `?${params.join("&")}`;
        return url2;
      }
      if (this.isMangaRaw()) {
        if (q)
          return `https://${this.domain}/search?keyword=${q}&page=${page}`;
        return `https://${this.domain}/?page=${page}`;
      }
      if (q) {
        return `https://${this.domain}${this.listUrl}?keyword=${q}&page=${page}`;
      }
      let url = `https://${this.domain}${this.listUrl}`;
      const tag = filter && filter.tags && filter.tags[0];
      if (tag)
        url += `/${tag.key}`;
      let sort = 0;
      switch (order) {
        case SortOrder.UPDATED:
          sort = 0;
          break;
        case SortOrder.POPULARITY:
          sort = 10;
          break;
        case SortOrder.NEWEST:
          sort = 15;
          break;
        case SortOrder.RATING:
          sort = 20;
          break;
        default:
          sort = 0;
      }
      url += `?sort=${sort}`;
      const state = filter && filter.states && filter.states[0];
      if (state) {
        url += `&status=${state === MangaState.ONGOING ? "1" : state === MangaState.FINISHED ? "2" : "-1"}`;
      }
      url += `&page=${page}`;
      return url;
    }
    async getListPage(page, order, filter = {}) {
      const url = this.buildListUrl(page, order, filter);
      let html;
      try {
        html = await this.context.httpGet(url, this);
      } catch {
        return [];
      }
      const doc = this.context.parseHTML(html);
      if (this.isMangaRaw())
        return this.parseMangaRawList(doc);
      return this.parseMangaList(doc);
    }
    // mangaraw.best: cards are "div.manga-vertical" with /raw/<slug> links and
    // "div.cover-frame img" covers (modern Tailwind markup, not WpComics).
    parseMangaRawList(doc) {
      const list = [];
      const seen = /* @__PURE__ */ new Set();
      const cards = this.queryAll(doc, ["div.manga-vertical", "div.cover-frame"]);
      for (const card of cards) {
        const a = this.queryFirst(card, ['a[href^="/raw/"]', "a"]);
        if (!a)
          continue;
        const href = this.toRelativeUrl(a.getAttribute("href") || "");
        const m = href.match(/^\/raw\/([^/]+)\/?$/);
        if (!m)
          continue;
        if (seen.has(href))
          continue;
        const img = this.queryFirst(card, ["div.cover-frame img", "img.cover", "img"]);
        const title = img && img.getAttribute("alt") || (this.queryFirst(card, [".latest-chapter a", "a.text-white"])?.textContent || "").trim() || a.textContent.trim();
        if (!title)
          continue;
        seen.add(href);
        list.push(new Manga({
          id: href,
          url: href,
          publicUrl: this.toAbsoluteUrl(href),
          coverUrl: this.imageSrc(img),
          title,
          source: this.source,
          contentRating: this.contentRating()
        }));
      }
      return list;
    }
    parseMangaList(doc) {
      let items = this.queryAll(doc, [
        "div.items div.item",
        "div.item-manga",
        "div.items article.item",
        "div.row div.item",
        "li.row"
      ]);
      const list = [];
      const seen = /* @__PURE__ */ new Set();
      for (const item of items) {
        const a = this.queryFirst(item, [
          "div.image > a",
          "div.image-item a",
          "figure figcaption h3 a",
          "figcaption h3 a",
          "h3 a",
          "a"
        ]);
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        if (!relHref || relHref === "/" || relHref.includes("#"))
          continue;
        if (seen.has(relHref))
          continue;
        const titleEl = this.queryFirst(item, [
          "div.box_tootip div.title",
          "h3 a",
          "h3",
          ".title"
        ]);
        const title = (titleEl ? titleEl.textContent : a.getAttribute("title") || a.textContent || "").trim();
        if (!title)
          continue;
        const img = this.queryFirst(item, [
          this.coverDiv,
          "div.image-item img",
          "div.image img",
          "img"
        ]);
        const tip = item.querySelector("div.box_tootip");
        let state;
        let author;
        if (tip) {
          const stateP = this.tipField(tip, "T\xECnh tr\u1EA1ng");
          if (stateP) {
            const v = stateP.toLowerCase();
            if (this.ongoing.has(v))
              state = MangaState.ONGOING;
            else if (this.finished.has(v))
              state = MangaState.FINISHED;
          }
          author = this.tipField(tip, "T\xE1c gi\u1EA3");
        }
        seen.add(relHref);
        list.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title,
          authors: author ? [author] : [],
          state,
          source: this.source,
          contentRating: this.contentRating()
        }));
      }
      return list;
    }
    /** Read "<label>: value" out of a box_tootip paragraph (own text). */
    tipField(tip, label) {
      const ps = Array.from(tip.querySelectorAll("div.message_main > p, p"));
      for (const p of ps) {
        if ((p.textContent || "").includes(label)) {
          const labelEl = p.querySelector("b, strong, span.name");
          let txt = p.textContent || "";
          if (labelEl)
            txt = txt.replace(labelEl.textContent || "", "");
          txt = txt.replace(label, "").replace(/^[:\s]+/, "").trim();
          if (txt)
            return txt;
        }
      }
      return null;
    }
    // ---- details + chapters ---------------------------------------------
    async getDetails(manga) {
      const fullUrl = this.toAbsoluteUrl(manga.url);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      if (this.isMangaRaw())
        return this.getMangaRawDetails(manga, doc);
      const descEl = this.queryFirst(doc, [
        "div.detail-content > div",
        "div.detail-content p",
        "div.summary-content p.detail-summary",
        "div.summary-content",
        this.selectDesc
      ]);
      const description = descEl ? descEl.innerHTML.trim() : manga.description || "";
      const altEl = this.queryFirst(doc, [
        "h2.other-name",
        "li.name-other.row p.detail-info"
      ]);
      const altTitle = altEl ? (altEl.textContent || "").trim() : "";
      const authorEl = this.queryFirst(doc, [
        "li.author.row p.detail-info",
        ...this.selectAut.split(",")
      ]);
      let author = authorEl ? (authorEl.textContent || "").trim() : "";
      if (author === "\u0110ang c\u1EADp nh\u1EADt")
        author = "";
      const stateEl = this.queryFirst(doc, [
        "li.status.row p.detail-info span.label",
        ...this.selectState.split(",")
      ]);
      let state;
      if (stateEl) {
        const v = (stateEl.textContent || "").toLowerCase().trim();
        if (this.ongoing.has(v))
          state = MangaState.ONGOING;
        else if (this.finished.has(v))
          state = MangaState.FINISHED;
      }
      const tags = this.parseTags(doc);
      const chapters = await this.loadChapters(manga, doc, html, fullUrl);
      return new Manga({
        ...manga,
        title: this.queryFirst(doc, ["h1.title-detail", "h1"])?.textContent?.trim() || manga.title,
        altTitles: altTitle ? [altTitle] : manga.altTitles || [],
        description,
        authors: author ? [author] : manga.authors || [],
        state: state || manga.state,
        tags: tags.length ? tags : manga.tags || [],
        rating: this.parseRating(doc) || manga.rating || 0,
        coverUrl: manga.coverUrl || this.imageSrc(this.queryFirst(doc, ["div.col-image img", "div.detail-info img", "img"])),
        source: this.source,
        contentRating: this.contentRating(),
        chapters
      });
    }
    parseTags(doc) {
      const els = this.queryAll(doc, [
        "li.kind p.col-xs-8 a",
        "li.category.row p.detail-info a[href*=tim-truyen]",
        "p.col-xs-12 a.tr-theloai",
        ...this.selectTag.split(",")
      ]);
      const tags = [];
      const seen = /* @__PURE__ */ new Set();
      for (const a of els) {
        const title = (a.textContent || "").trim();
        const href = a.getAttribute("href") || "";
        const key = href.replace(/\/$/, "").split("/").pop();
        if (!title || !key || seen.has(key))
          continue;
        seen.add(key);
        tags.push({ title, key });
      }
      return tags;
    }
    async loadChapters(manga, doc, html, fullUrl) {
      const id = this.source && this.source.id || "";
      const slug = (this.toRelativeUrl(manga.url) || "").replace(/\/$/, "").split("/").pop();
      if (id === "NETTRUYEN" || id === "NETTRUYENVIE" || id === "NHATTRUYENVN") {
        const apiChapters = await this.fetchAsmxChapters(id, slug);
        if (apiChapters && apiChapters.length)
          return apiChapters;
      }
      if (id === "NEWTRUYEN") {
        const storyId = this.queryFirst(doc, ["input#storyID"])?.getAttribute("value");
        if (storyId) {
          const apiChapters = await this.fetchStoryIdChapters(storyId);
          if (apiChapters && apiChapters.length)
            return apiChapters;
        }
      }
      if (id === "XOXOCOMICS" || /xoxocomic/i.test(this.domain)) {
        const paged = await this.fetchXoxoChapters(fullUrl);
        if (paged && paged.length)
          return paged;
      }
      return this.parseChaptersFromDoc(doc);
    }
    async fetchAsmxChapters(id, slug) {
      try {
        let apiUrl;
        if (id === "NETTRUYEN") {
          const raw = slug || "";
          const realSlug = raw.replace(/-\d+$/, "");
          const comicId = (raw.match(/-(\d+)$/) || [, raw.split("-").pop()])[1];
          apiUrl = `https://${this.domain}/Comic/Services/ComicService.asmx/ChapterList?slug=${realSlug}&comicId=${comicId}`;
        } else {
          apiUrl = `https://${this.domain}/Comic/Services/ComicService.asmx/ChapterList?slug=${slug}`;
        }
        const text = await this.context.httpGet(apiUrl, this);
        const json = JSON.parse(text);
        const data = json.data || [];
        const n = data.length;
        const chapters = [];
        for (let i = 0; i < n; i++) {
          const jo = data[n - 1 - i];
          const chapterSlug = jo.chapter_slug;
          let chapterUrl;
          if (id === "NETTRUYEN") {
            const realSlug = (slug || "").replace(/-\d+$/, "");
            chapterUrl = `/truyen-tranh/${realSlug}/${chapterSlug}/${jo.chapter_id}`;
          } else {
            chapterUrl = `/truyen-tranh/${slug}/${chapterSlug}`;
          }
          const num = parseFloat(jo.chapter_num) || i + 1;
          chapters.push(new MangaChapter({
            id: chapterUrl,
            url: chapterUrl,
            title: jo.chapter_name || `Chapter ${chapterSlug}`,
            number: num,
            uploadDate: this.parseChapterDate(jo.updated_at),
            source: this.source
          }));
        }
        return chapters;
      } catch {
        return null;
      }
    }
    async fetchStoryIdChapters(storyId) {
      try {
        const url = `https://${this.domain}/Story/ListChapterByStoryID?storyID=${storyId}`;
        const html = await this.context.httpGet(url, this);
        const doc = this.context.parseHTML(html);
        const lis = this.queryAll(doc, ["li.row", "div.list-chapter li.row"]);
        const out = [];
        for (const li of lis) {
          const a = this.queryFirst(li, ["div.col-xs-5.chapter a", "div.chapter a", "a"]);
          if (!a)
            continue;
          const href = this.toRelativeUrl(a.getAttribute("href"));
          if (!href)
            continue;
          const dateText = this.queryFirst(li, ["div.col-xs-4.text-center.small", "div.col-xs-4"])?.textContent;
          out.push({ href, title: (a.textContent || "").trim(), dateText });
        }
        out.reverse();
        return out.map((c, i) => new MangaChapter({
          id: c.href,
          url: c.href,
          title: c.title,
          number: i + 1,
          uploadDate: this.parseChapterDate(c.dateText),
          source: this.source
        }));
      } catch {
        return null;
      }
    }
    async fetchXoxoChapters(baseUrl) {
      const collected = [];
      for (let page = 1; page <= 50; page++) {
        let html;
        try {
          html = await this.context.httpGet(`${baseUrl}?page=${page}`, this);
        } catch {
          break;
        }
        const doc = this.context.parseHTML(html);
        const lis = this.queryAll(doc, ["#nt_listchapter nav ul li:not(.heading)", "#nt_listchapter li:not(.heading)"]);
        if (!lis.length)
          break;
        for (const li of lis) {
          const a = this.queryFirst(li, ["a"]);
          if (!a)
            continue;
          const href = this.toRelativeUrl(a.getAttribute("href"));
          if (!href)
            continue;
          const dateText = this.queryFirst(li, ["div.col-xs-3"])?.textContent;
          collected.push({ href, title: (a.textContent || "").trim(), dateText });
        }
      }
      collected.reverse();
      return collected.map((c, i) => new MangaChapter({
        id: c.href,
        url: c.href,
        title: c.title,
        number: i + 1,
        uploadDate: this.parseChapterDate(c.dateText),
        source: this.source
      }));
    }
    // mangaraw.best details: title h1, chapters are <a href="/raw/<slug>/<chap>">
    // wrapping an <li> whose span.text-ellipsis holds the chapter name and
    // span.timeago a relative (Japanese) date. Genre tags via /genre/<key> links.
    getMangaRawDetails(manga, doc) {
      const slug = (this.toRelativeUrl(manga.url) || "").replace(/\/$/, "").split("/").pop();
      const seriesPrefix = `/raw/${slug}/`;
      const title = this.queryFirst(doc, ["h1", "h1.text-2xl"])?.textContent?.trim() || manga.title;
      const tags = [];
      const seenTag = /* @__PURE__ */ new Set();
      for (const a of this.queryAll(doc, ['a[href*="/genre/"]'])) {
        const key = (a.getAttribute("href") || "").replace(/\/$/, "").split("/").pop();
        const tTitle = (a.textContent || "").trim();
        if (!key || !tTitle || seenTag.has(key))
          continue;
        seenTag.add(key);
        tags.push({ title: tTitle, key });
      }
      const cover = this.imageSrc(this.queryFirst(doc, ["img.cover", "div.cover-frame img", "img[alt]"]));
      const anchors = this.queryAll(doc, [`a[href^="${seriesPrefix}"]`]);
      const rows = [];
      const seen = /* @__PURE__ */ new Set();
      for (const a of anchors) {
        const href = this.toRelativeUrl(a.getAttribute("href") || "");
        if (!href.startsWith(seriesPrefix))
          continue;
        if (seen.has(href))
          continue;
        const li = a.querySelector("li");
        const nameEl = (li || a).querySelector("span.text-ellipsis");
        if (!nameEl)
          continue;
        seen.add(href);
        const dateText = (li || a).querySelector("span.timeago")?.textContent;
        rows.push({ href, title: (nameEl.textContent || "").trim(), dateText });
      }
      rows.reverse();
      const chapters = rows.map((c, i) => {
        const numMatch = c.title.match(/(\d+(?:\.\d+)?)/);
        return new MangaChapter({
          id: c.href,
          url: c.href,
          title: c.title || `Chapter ${i + 1}`,
          number: numMatch ? parseFloat(numMatch[1]) : i + 1,
          uploadDate: this.parseChapterDate(c.dateText),
          source: this.source
        });
      });
      return new Manga({
        ...manga,
        title,
        coverUrl: manga.coverUrl || cover,
        tags: tags.length ? tags : manga.tags || [],
        source: this.source,
        contentRating: this.contentRating(),
        chapters
      });
    }
    parseChaptersFromDoc(doc) {
      let lis = this.queryAll(doc, [
        this.selectChapter,
        "div.list-chapter li.row:not(.heading)",
        "div.list_chapter div.row:not(.heading)",
        "li.row:not(.heading)",
        "ul.list-chapter li",
        "div.list-chapter li"
      ]).filter((li) => {
        const style = li.getAttribute && li.getAttribute("style");
        return !(style && /display:\s*none/.test(style));
      });
      const out = [];
      for (const li of lis) {
        const a = this.queryFirst(li, ["a.chapter", "div.chapter a", "a"]);
        if (!a)
          continue;
        const href = this.toRelativeUrl(a.getAttribute("href"));
        if (!href || href.includes("#"))
          continue;
        const dateText = this.queryFirst(li, [
          "div.style-chap",
          this.selectDate,
          "div.col-xs-4",
          "div.col-xs-3"
        ])?.textContent;
        const dataNum = a.getAttribute && a.getAttribute("data-chapter");
        out.push({
          href,
          title: (a.textContent || "").trim(),
          dateText,
          dataNum: dataNum ? parseFloat(dataNum) : null
        });
      }
      out.reverse();
      return out.map((c, i) => new MangaChapter({
        id: c.href,
        url: c.href,
        title: c.title || `Chapter ${i + 1}`,
        number: c.dataNum != null && !Number.isNaN(c.dataNum) ? c.dataNum : i + 1,
        uploadDate: this.parseChapterDate(c.dateText),
        source: this.source
      }));
    }
    // ---- pages -----------------------------------------------------------
    async getPages(chapter) {
      let fullUrl = this.toAbsoluteUrl(chapter.url);
      if ((this.source && this.source.id) === "XOXOCOMICS" || /xoxocomic/i.test(this.domain)) {
        fullUrl = fullUrl.replace(/\/$/, "") + "/all";
      }
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      if (this.isMangaRaw())
        return this.getMangaRawPages(html);
      const imgs = this.queryAll(doc, [
        this.selectPage,
        "div.page-chapter > img",
        "div.page-chapter img",
        "li.blocks-gallery-item img",
        "div.reading-detail img",
        "div.reading img",
        "img[data-original]"
      ]);
      const pages = [];
      const seen = /* @__PURE__ */ new Set();
      for (const img of imgs) {
        let url = this.imageSrc(img);
        if (!url)
          continue;
        url = url.replace(/[\[\]]/g, "");
        if (this.adFragments.some((f) => url.includes(f)))
          continue;
        if (seen.has(url))
          continue;
        seen.add(url);
        pages.push(new MangaPage({
          id: url,
          url,
          source: this.source
        }));
      }
      return pages;
    }
    // mangaraw.best reader serves page images directly as <img src> from its CDN
    // host (e.g. rbest.mgcdnxyz.cfd/<cover-uuid>/<chapter-uuid>/<n>.jpg). Extract
    // them from the HTML and sort numerically by trailing page number so order is
    // correct regardless of DOM ordering.
    getMangaRawPages(html) {
      const found = /* @__PURE__ */ new Map();
      const re = /https?:\/\/[^"'\s)]*\/(\d+)\.(?:jpe?g|png|webp)/gi;
      let m;
      while ((m = re.exec(html)) !== null) {
        const url = m[0];
        if (/\/(avatars?|default|covers?|logo|credit|images\/pets)\//i.test(url))
          continue;
        if (!found.has(url))
          found.set(url, parseInt(m[1], 10));
      }
      const entries = Array.from(found.entries()).sort((a, b) => a[1] - b[1]);
      return entries.map(([url]) => new MangaPage({
        id: url,
        url,
        source: this.source
      }));
    }
  };

  // src/pizzareader.js
  var PizzaReaderParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 9999) {
      super(context, source, domain, pageSize);
      this.apiPrefix = "/api";
      this.comicsPath = "/api/comics";
      this.searchPath = "/api/search/";
      this.ongoing = /* @__PURE__ */ new Set([
        "en cours",
        "in corso",
        "in corso (cadenza irregolare)",
        "in corso (irregolare)",
        "in corso (mensile)",
        "in corso (quindicinale)",
        "in corso (settimanale)",
        "in corso (bisettimanale)"
      ]);
      this.finished = /* @__PURE__ */ new Set([
        "termin\xE9",
        "concluso",
        "completato"
      ]);
      this.paused = /* @__PURE__ */ new Set([
        "in pausa",
        "in corso (in pausa)"
      ]);
      this.abandoned = /* @__PURE__ */ new Set([
        "droppato"
      ]);
      this.ongoingFilter = "in corso";
      this.completedFilter = "concluso";
      this.hiatusFilter = "in pausa";
      this.abandonedFilter = "droppato";
    }
    async getJson(url) {
      const text = await this.context.httpGet(url, this);
      return JSON.parse(text);
    }
    parseDate(value) {
      if (!value)
        return 0;
      const t = Date.parse(value);
      return Number.isNaN(t) ? 0 : t;
    }
    mapState(statusRaw) {
      const status = String(statusRaw || "").toLowerCase();
      if (this.ongoing.has(status))
        return MangaState.ONGOING;
      if (this.finished.has(status))
        return MangaState.FINISHED;
      if (this.paused.has(status))
        return MangaState.PAUSED;
      if (this.abandoned.has(status))
        return MangaState.ABANDONED;
      return void 0;
    }
    // Build a Manga from a catalog/search JSON object. The `url` field from the
    // API lacks the `/api` prefix; we store the `/api...` path as id/url so
    // getDetails can fetch the JSON endpoint directly.
    buildManga(j) {
      const apiUrl = this.apiPrefix + (j.url || "");
      const adult = Number(j.adult);
      const isNsfw = adult === 0 ? false : true;
      const author = j.author;
      const rating = (() => {
        const r = parseFloat(j.rating);
        return Number.isNaN(r) ? 0 : r / 10;
      })();
      let altTitles = [];
      if (Array.isArray(j.alt_titles))
        altTitles = j.alt_titles.filter(Boolean);
      return new Manga({
        id: apiUrl,
        url: apiUrl,
        publicUrl: this.toAbsoluteUrl(j.url || apiUrl),
        coverUrl: j.thumbnail || j.thumbnail_small || "",
        largeCoverUrl: j.thumbnail || j.thumbnail_small || "",
        title: j.title || "",
        altTitles,
        description: j.description || "",
        rating,
        tags: Array.isArray(j.genres) ? j.genres.filter(Boolean).map((g) => ({ key: g.slug || g.name, title: g.name })) : [],
        authors: author ? [author] : [],
        state: this.mapState(j.status),
        source: this.source,
        isNsfw,
        contentRating: isNsfw ? ContentRating.ADULT : ContentRating.SAFE
      });
    }
    async getListPage(page, order, filter) {
      filter = filter || {};
      if (page && page > 1 && !filter.query)
        return [];
      let comics = [];
      if (filter.query) {
        const data = await this.getJson(`https://${this.domain}${this.searchPath}${encodeURIComponent(filter.query)}`);
        comics = Array.isArray(data.comics) ? data.comics : [];
      } else {
        const data = await this.getJson(`https://${this.domain}${this.comicsPath}`);
        comics = Array.isArray(data.comics) ? data.comics : [];
      }
      const tags = (filter.tags || []).map((t) => String(t.key || t.title || t).toLowerCase()).filter(Boolean);
      const tagsExclude = (filter.tagsExclude || []).map((t) => String(t.key || t.title || t).toLowerCase()).filter(Boolean);
      const states = filter.states || [];
      const result = [];
      for (const j of comics) {
        if (!filter.query) {
          if (tags.length) {
            const genreStr = JSON.stringify(j.genres || []).toLowerCase();
            if (!tags.some((k) => genreStr.includes(k)))
              continue;
          }
          if (tagsExclude.length) {
            const genreStr = JSON.stringify(j.genres || []).toLowerCase();
            if (!tagsExclude.some((k) => !genreStr.includes(k)))
              continue;
          }
          if (states.length === 1) {
            const statusStr = String(j.status || "").toLowerCase();
            const want = states[0];
            let frag = "";
            if (want === MangaState.PAUSED)
              frag = this.hiatusFilter;
            else if (want === MangaState.ONGOING)
              frag = this.ongoingFilter;
            else if (want === MangaState.FINISHED)
              frag = this.completedFilter;
            else if (want === MangaState.ABANDONED)
              frag = this.abandonedFilter;
            if (frag && !statusStr.includes(frag.toLowerCase()))
              continue;
          }
        }
        result.push(this.buildManga(j));
      }
      return result;
    }
    async getDetails(manga) {
      const fullUrl = this.toAbsoluteUrl(manga.url);
      const data = await this.getJson(fullUrl);
      const comic = data.comic || {};
      const tags = Array.isArray(comic.genres) ? comic.genres.filter(Boolean).map((g) => ({ key: g.slug || g.name, title: g.name })) : manga.tags;
      const rawChapters = Array.isArray(comic.chapters) ? comic.chapters.slice().reverse() : [];
      const chapters = rawChapters.map((j, i) => {
        const apiUrl = this.apiPrefix + (j.url || "");
        return new MangaChapter({
          id: apiUrl,
          url: apiUrl,
          title: j.full_title || j.title || "",
          number: i + 1,
          volume: Number(j.volume) || 0,
          branch: null,
          uploadDate: this.parseDate(j.updated_at || j.published_on),
          scanlator: Array.isArray(j.teams) && j.teams.length ? j.teams.map((t) => t && (t.name || (typeof t === "string" ? t : "")) || "").filter(Boolean).join(", ") || null : null,
          source: this.source,
          index: i
        });
      });
      return new Manga({
        ...manga,
        title: comic.title || manga.title,
        description: comic.description || manga.description,
        coverUrl: comic.thumbnail || manga.coverUrl,
        largeCoverUrl: comic.thumbnail || manga.largeCoverUrl || manga.coverUrl,
        tags,
        chapters
      });
    }
    async getPages(chapter) {
      const fullUrl = this.toAbsoluteUrl(chapter.url);
      const data = await this.getJson(fullUrl);
      const ch = data.chapter || {};
      const pages = Array.isArray(ch.pages) ? ch.pages : [];
      return pages.filter(Boolean).map((url, i) => new MangaPage({
        id: url,
        url: this.toAbsoluteUrl(url),
        preview: null,
        source: this.source
      }));
    }
  };

  // src/keyoapp.js
  var KeyoappParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 100) {
      super(context, source, domain, pageSize);
      this.listUrl = "series/";
      this.latestPath = "latest";
      this.datePattern = "MMM d, yyyy";
      this.selectMangaList = "div.grid > div.group";
      this.selectMangaSearch = "#searched_series_page button";
      this.selectTitle = "h3";
      this.selectDesc = "div.grid > div.overflow-hidden > p";
      this.selectState = "div[alt=Status]";
      this.selectTag = "div.grid:has(>h1) > div > a";
      this.selectAuthor = "div[alt=Author]";
      this.selectChapter = "#chapters > a";
      this.selectPage = "#pages > img";
      this.cdnRegex = /realUrl\s*=\s*`[^`]+\/\/([^/`]+)/;
      this.cdnUploadsPath = "/uploads";
      this.ongoing = /* @__PURE__ */ new Set(["ongoing", "on going"]);
      this.finished = /* @__PURE__ */ new Set(["completed", "complete", "finished", "end"]);
      this.paused = /* @__PURE__ */ new Set(["paused", "hiatus", "on hold"]);
      this.upcoming = /* @__PURE__ */ new Set(["dropped", "upcoming", "coming soon"]);
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    queryFirst(el, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const found = el.querySelector(selector);
          if (found)
            return found;
        } catch {
        }
      }
      return null;
    }
    // Pull a background-image url(...) out of an element's inline style.
    cssBackgroundUrl(el) {
      if (!el)
        return "";
      const style = el.getAttribute("style") || "";
      const m = style.match(/background-image\s*:\s*url\(\s*['"]?([^'")]+)['"]?\s*\)/i);
      return m ? m[1].trim() : "";
    }
    coverOf(div) {
      const candidates = [
        "a div.bg-cover",
        "div.bg-cover",
        "a.bg-cover",
        "[style*=background-image]"
      ];
      for (const sel of candidates) {
        let el;
        try {
          el = div.querySelector(sel);
        } catch {
          el = null;
        }
        const url = this.cssBackgroundUrl(el);
        if (url)
          return this.toAbsoluteUrl(url);
      }
      return "";
    }
    titleOf(div) {
      const h = this.queryFirst(div, [this.selectTitle, "h3", "h2", "span.text-sm"]);
      const fromHeading = h && h.textContent ? h.textContent.trim() : "";
      if (fromHeading)
        return fromHeading;
      const a = div.querySelector("a");
      const attr = (el) => (el && (el.getAttribute("title") || el.getAttribute("alt")) || "").trim();
      return attr(div) || attr(a) || "";
    }
    parseTags(scope) {
      const out = [];
      const seen = /* @__PURE__ */ new Set();
      const anchors = this.queryAll(scope, [
        this.selectTag,
        "div.grid:has(>h1) > div > a",
        "div.gap-1 a",
        "a[href*='genre=']",
        "a[href*='tag=']"
      ]);
      for (const a of anchors) {
        const title = (a.textContent || "").trim();
        if (!title)
          continue;
        const href = a.getAttribute("href") || "";
        const key = (href.split("=").pop() || title).trim() || title;
        if (seen.has(key))
          continue;
        seen.add(key);
        out.push({ key, title, source: this.source });
      }
      return out;
    }
    contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    async getListPage(page, order, filter) {
      filter = filter || {};
      if (page && page > 1)
        return [];
      const segment = order === SortOrder.UPDATED ? this.latestPath : "series";
      const url = `https://${this.domain}/${segment}`;
      const html = await this.context.httpGet(url, this);
      const tag = filter.tags && filter.tags.length === 1 ? filter.tags[0].title || filter.tags[0].key || "" : "";
      return this.parseMangaList(html, tag, (filter.query || "").trim());
    }
    parseMangaList(html, tag, query) {
      const doc = this.context.parseHTML(html);
      let elements = this.queryAll(doc, [this.selectMangaSearch, "#searched_series_page button"]);
      if (!elements.length) {
        elements = this.queryAll(doc, [
          this.selectMangaList,
          "div.grid > div.group",
          "div.grid > button.group",
          ".grid .group"
        ]);
      }
      const out = [];
      const seen = /* @__PURE__ */ new Set();
      for (const div of elements) {
        const a = div.querySelector("a") || (div.tagName === "A" ? div : null);
        const href = a ? a.getAttribute("href") : div.getAttribute("href") || "";
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        if (!/\/series\//.test(relHref) || /\/chapter\//.test(relHref))
          continue;
        if (seen.has(relHref))
          continue;
        const title = this.titleOf(div);
        if (query && !(title && title.toLowerCase().includes(query.toLowerCase()))) {
          const tagsAttr = (div.getAttribute("tags") || "").toLowerCase();
          if (!tagsAttr.includes(query.toLowerCase()))
            continue;
        }
        if (tag) {
          const tagsAttr = div.getAttribute("tags") || "";
          const tagsText = tagsAttr || this.parseTags(div).map((t) => t.title).join(",");
          if (!tagsText.toLowerCase().includes(tag.toLowerCase()))
            continue;
        }
        seen.add(relHref);
        out.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.coverOf(div),
          title,
          tags: this.parseTags(div),
          source: this.source,
          contentRating: this.contentRating()
        }));
      }
      return out;
    }
    parseState(text) {
      const t = (text || "").trim().toLowerCase();
      if (!t)
        return void 0;
      if (this.ongoing.has(t))
        return MangaState.ONGOING;
      if (this.finished.has(t))
        return MangaState.FINISHED;
      if (this.paused.has(t))
        return MangaState.PAUSED;
      if (this.upcoming.has(t))
        return MangaState.UPCOMING;
      return void 0;
    }
    findState(doc) {
      const el = this.queryFirst(doc, [this.selectState, "div[alt=Status]", "div[alt='Status']"]);
      let state = el ? this.parseState(el.textContent) : void 0;
      if (state)
        return state;
      for (const chip of this.queryAll(doc, ["[data-status]", "span", "div"])) {
        const dataStatus = chip.getAttribute && chip.getAttribute("data-status");
        state = this.parseState(dataStatus || chip.textContent);
        if (state)
          return state;
      }
      return void 0;
    }
    chapterTitle(a) {
      const span = this.queryFirst(a, ["span.truncate", "span.text-sm.truncate", "span.text-sm > span", ".chapternum"]);
      const fromSpan = span && span.textContent ? span.textContent.trim() : "";
      if (fromSpan)
        return fromSpan;
      const attr = (a.getAttribute("title") || a.getAttribute("alt") || "").trim();
      if (attr)
        return attr;
      return (a.textContent || "").replace(/\s+/g, " ").trim();
    }
    chapterDate(a) {
      const attr = (a.getAttribute("d") || "").trim();
      if (attr)
        return this.parseChapterDate(attr);
      const dateEls = this.queryAll(a, ["div.text-xs.w-fit", "div.text-sm.w-fit", "div.w-fit"]);
      const last = dateEls.length ? dateEls[dateEls.length - 1] : null;
      return this.parseChapterDate(last ? (last.textContent || "").trim() : "");
    }
    isUpcoming(a) {
      const text = a.textContent || "";
      if (/\bupcoming\b/i.test(text)) {
        const span = a.querySelector(".text-sm span, .text-sm");
        if (span && /upcoming/i.test(span.textContent || ""))
          return true;
      }
      return false;
    }
    async getDetails(manga) {
      const fullUrl = this.toAbsoluteUrl(manga.url);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      const descEl = this.queryFirst(doc, [
        this.selectDesc,
        "div.grid > div.overflow-hidden > p",
        ".overflow-hidden p",
        "div[alt=Description]"
      ]);
      const description = descEl ? (descEl.innerHTML || descEl.textContent || "").trim() : manga.description || "";
      const title = (doc.querySelector("h1")?.textContent || "").trim() || manga.title;
      const tags = this.parseTags(doc);
      const isChapterHref = (href) => /\/chapter[/\-]/i.test(href) || /\/chapter\//i.test(href);
      let chapterAnchors = this.queryAll(doc, [
        this.selectChapter,
        "#chapters > a",
        "#chapters a[href*='/chapter']"
      ]).filter((a) => {
        const href = a.getAttribute("href") || "";
        return isChapterHref(href) && !this.isUpcoming(a);
      });
      if (!chapterAnchors.length) {
        chapterAnchors = this.queryAll(doc, ["a[href*='/chapter']"]).filter((a) => {
          const href = a.getAttribute("href") || "";
          return isChapterHref(href) && !this.isUpcoming(a);
        });
      }
      const seen = /* @__PURE__ */ new Set();
      const ordered = [];
      for (const a of chapterAnchors) {
        const rel = this.toRelativeUrl(a.getAttribute("href"));
        if (seen.has(rel))
          continue;
        seen.add(rel);
        ordered.push(a);
      }
      const total = ordered.length;
      const chapters = ordered.map((a, idx) => {
        const rel = this.toRelativeUrl(a.getAttribute("href"));
        const i = total - 1 - idx;
        const urlNum = (rel.match(/chapter[/\-]([\d.]+)/i) || [])[1];
        return new MangaChapter({
          id: rel,
          url: rel,
          title: this.chapterTitle(a),
          number: urlNum ? parseFloat(urlNum) : i + 1,
          volume: 0,
          uploadDate: this.chapterDate(a),
          source: this.source
        });
      }).reverse();
      return new Manga({
        ...manga,
        title,
        description: description || manga.description || "",
        tags: tags.length ? tags : manga.tags,
        state: this.findState(doc) || manga.state,
        contentRating: this.contentRating(),
        chapters
      });
    }
    getCdnUrl(doc) {
      const scripts = Array.from(doc.querySelectorAll("script"));
      for (const s of scripts) {
        const code = s.textContent || "";
        const m = code.match(this.cdnRegex);
        if (m && m[1]) {
          return `https://${m[1]}${this.cdnUploadsPath}`;
        }
      }
      return null;
    }
    async getPages(chapter) {
      const fullUrl = this.toAbsoluteUrl(chapter.url);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      const imgs = this.queryAll(doc, [this.selectPage, "#pages > img", "#pages img"]);
      const cdnUrl = this.getCdnUrl(doc);
      if (cdnUrl) {
        const pages2 = imgs.map((img) => (img.getAttribute("uid") || "").trim()).filter(Boolean).map((uid) => {
          const url = `${cdnUrl}/${uid}`;
          return new MangaPage({ id: url, url, source: this.source });
        });
        if (pages2.length)
          return pages2;
      }
      const fromPagesImgs = imgs.map((img) => this.directImageUrl(img)).filter(Boolean);
      if (fromPagesImgs.length) {
        return fromPagesImgs.map((url) => new MangaPage({ id: url, url, source: this.source }));
      }
      const readerImgs = Array.from(doc.querySelectorAll("img")).map((img) => this.directImageUrl(img)).filter((url) => url && /(storage|cdn|uploads?|\/series\/|\/chapter)/i.test(url) && /\.(webp|jpe?g|png|avif)(\?|$)/i.test(url.replace(/.*?url=/i, "")));
      const seen = /* @__PURE__ */ new Set();
      const pages = [];
      for (const url of readerImgs) {
        if (seen.has(url))
          continue;
        seen.add(url);
        pages.push(new MangaPage({ id: url, url, source: this.source }));
      }
      return pages;
    }
    directImageUrl(img) {
      if (!img)
        return "";
      const raw = img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "";
      if (!raw || raw.startsWith("data:") || raw.startsWith("blob:"))
        return "";
      if (/placeholder|favicon|apple-touch|logo|iconify/i.test(raw))
        return "";
      return this.toAbsoluteUrl(raw);
    }
    // ----- Date parsing (port of KeyoappParser.parseChapterDate) -----
    parseChapterDate(date) {
      if (!date)
        return 0;
      const d = date.toLowerCase().trim();
      if (/\bago\b/.test(d))
        return this.parseRelativeDate(d);
      if (d.startsWith("today")) {
        const now = /* @__PURE__ */ new Date();
        now.setHours(0, 0, 0, 0);
        return now.getTime();
      }
      let cleaned = date;
      if (/\d(st|nd|rd|th)/i.test(date)) {
        cleaned = date.split(" ").map((tok) => /\d\D\D/.test(tok) ? tok.replace(/\D/g, "") : tok).join(" ");
      }
      const ts = Date.parse(cleaned.replace(/,/g, ""));
      return Number.isNaN(ts) ? 0 : ts;
    }
    parseRelativeDate(date) {
      const m = date.match(/(\d+)/);
      const n = m ? parseInt(m[1], 10) : 0;
      if (!n)
        return 0;
      const now = /* @__PURE__ */ new Date();
      if (/second/.test(date))
        now.setSeconds(now.getSeconds() - n);
      else if (/minute/.test(date))
        now.setMinutes(now.getMinutes() - n);
      else if (/hour/.test(date))
        now.setHours(now.getHours() - n);
      else if (/day/.test(date))
        now.setDate(now.getDate() - n);
      else if (/week/.test(date))
        now.setDate(now.getDate() - n * 7);
      else if (/month/.test(date))
        now.setMonth(now.getMonth() - n);
      else if (/year/.test(date))
        now.setFullYear(now.getFullYear() - n);
      else
        return 0;
      return now.getTime();
    }
  };

  // src/foolslide.js
  var FoolSlideParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 25) {
      super(context, source, domain, pageSize);
      this.listUrl = "directory/";
      this.searchUrl = "search/";
      this.pagination = true;
      this.datePattern = "yyyy.MM.dd";
      this.selectInfo = "div.info";
      this.selectChapter = "div.list div.element";
      this.selectDate = ".meta_r";
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    async getListPage(page, order, filter) {
      filter = filter || {};
      const query = filter.query;
      let html;
      if (query) {
        if (page > 1)
          return [];
        const url = `https://${this.domain}/${this.searchUrl}`;
        const body = `search=${encodeURIComponent(query)}`;
        html = await this.context.httpPost(url, body, {
          "Content-Type": "application/x-www-form-urlencoded"
        }, this);
      } else {
        let url = `https://${this.domain}/${this.listUrl}`;
        if (this.pagination) {
          url += String(page);
        } else if (page > 1) {
          return [];
        }
        html = await this.context.httpGet(url, this);
      }
      return this.parseMangaList(html);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, [
        "div.list div.group",
        "div.list .group",
        ".group"
      ]);
      const mangaList = [];
      for (const div of elements) {
        const a = div.querySelector(".title a") || div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const img = div.querySelector("img");
        const titleEl = div.querySelector(".title a");
        const title = (titleEl ? titleEl.textContent : a.textContent || "").trim();
        mangaList.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title,
          source: this.source,
          contentRating: this.contentRating()
        }));
      }
      return mangaList;
    }
    async getDetails(manga) {
      const fullUrl = this.toAbsoluteUrl(manga.url);
      let html = await this.context.httpGet(fullUrl, this);
      let doc = this.context.parseHTML(html);
      const adultForm = doc.querySelector("div.info form");
      if (adultForm) {
        try {
          html = await this.context.httpPost(fullUrl, "adult=true", {
            "Content-Type": "application/x-www-form-urlencoded"
          }, this);
          doc = this.context.parseHTML(html);
        } catch {
        }
      }
      const chapters = this.parseChapters(doc);
      const infoEl = doc.querySelector(this.selectInfo);
      const infoHtml = infoEl ? infoEl.innerHTML : "";
      const infoText = infoEl ? infoEl.textContent.replace(/\s+/g, " ").trim() : "";
      let desc = "";
      let author = null;
      if (infoHtml.includes("Description")) {
        desc = infoText.split("Description: ")[1] || infoText;
        desc = desc.split("Readings")[0].trim();
      } else if (infoHtml.includes("</b>")) {
        const parts = infoText.split(": ");
        desc = (parts.length ? parts[parts.length - 1] : infoText).trim();
      } else {
        desc = infoText;
      }
      if (infoHtml.includes("Author")) {
        author = ((infoText.split("Author: ")[1] || "").split("Art")[0] || "").trim();
      } else if (infoHtml.includes("</b>")) {
        author = ((infoText.split(": ")[1] || "").split("Art")[0] || "").trim();
      }
      const cover = this.imageSrc(doc.querySelector(".thumbnail img")) || manga.coverUrl || "";
      return new Manga({
        ...manga,
        coverUrl: cover,
        largeCoverUrl: cover || manga.largeCoverUrl || manga.coverUrl,
        description: desc || manga.description || "",
        authors: author ? [author] : manga.authors || [],
        contentRating: this.contentRating(),
        source: this.source,
        chapters
      });
    }
    parseChapters(doc) {
      const elements = this.queryAll(doc, [
        this.selectChapter,
        "div.list div.element",
        "div.list .element",
        ".element"
      ]);
      const reversed = elements.slice().reverse();
      return reversed.map((div, i) => {
        const a = div.querySelector(".title a") || div.querySelector("a");
        if (!a)
          return null;
        const href = a.getAttribute("href");
        if (!href)
          return null;
        const relHref = this.toRelativeUrl(href);
        const dateEl = div.querySelector(this.selectDate);
        const dateRaw = dateEl ? dateEl.textContent : "";
        let uploadDate = 0;
        if (dateRaw && dateRaw.includes(", ")) {
          const dateText = dateRaw.split(", ").slice(1).join(", ").trim();
          const parsed = Date.parse(dateText.replace(/\./g, "/"));
          if (!Number.isNaN(parsed))
            uploadDate = parsed;
        }
        return new MangaChapter({
          id: relHref,
          url: relHref,
          title: a.textContent.trim(),
          number: i + 1,
          volume: 0,
          uploadDate,
          source: this.source,
          scanlator: null,
          branch: null
        });
      }).filter(Boolean);
    }
    async getPages(chapter) {
      const fullUrl = this.toAbsoluteUrl(chapter.url);
      let html = await this.context.httpGet(fullUrl, this);
      let arrText = this.extractPagesArray(html);
      if (!arrText && (html.includes("adult=true") || /Adult content/i.test(html))) {
        try {
          html = await this.context.httpPost(fullUrl, "adult=true", {
            "Content-Type": "application/x-www-form-urlencoded"
          }, this);
          arrText = this.extractPagesArray(html);
        } catch {
        }
      }
      if (!arrText)
        return [];
      let images;
      try {
        images = JSON.parse(arrText);
      } catch {
        return [];
      }
      if (!Array.isArray(images))
        return [];
      return images.map((p, i) => {
        const url = p && p.url ? this.toAbsoluteUrl(p.url) : "";
        return new MangaPage({
          id: url || String(i),
          url,
          source: this.source
        });
      }).filter((p) => p.url);
    }
    // Pull the JSON array literal out of `var pages = [...]` (last occurrence,
    // matching balanced brackets up to the terminating ';').
    extractPagesArray(html) {
      const marker = "var pages = ";
      const idx = html.lastIndexOf(marker);
      if (idx < 0)
        return null;
      const start = html.indexOf("[", idx);
      if (start < 0)
        return null;
      let depth = 0;
      let inStr = false;
      let strCh = "";
      for (let i = start; i < html.length; i++) {
        const ch = html[i];
        if (inStr) {
          if (ch === "\\") {
            i++;
            continue;
          }
          if (ch === strCh)
            inStr = false;
          continue;
        }
        if (ch === '"' || ch === "'") {
          inStr = true;
          strCh = ch;
          continue;
        }
        if (ch === "[")
          depth++;
        else if (ch === "]") {
          depth--;
          if (depth === 0)
            return html.slice(start, i + 1);
        }
      }
      return null;
    }
  };

  // src/liliana.js
  var LilianaParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 24) {
      super(context, source, domain, pageSize);
      this.searchPath = "/search/";
      this.filterPath = "/filter/";
      this.selectMangaList = "div#main div.grid > div";
      this.selectMangaListTitle = ".text-center a";
      this.selectDescription = "div#syn-target";
      this.selectCover = ".a1 > figure img";
      this.selectTag = ".a2 div > a[rel='tag'].label";
      this.selectAuthor = "div.y6x11p i.fas.fa-user + span.dt";
      this.selectState = "div.y6x11p i.fas.fa-rss + span.dt";
      this.selectChapter = "ul > li.chapter";
      this.chapterIdMarker = "const CHAPTER_ID = ";
      this.ajaxImageListPath = "/ajax/image/list/chap/";
      this.selectPageContainer = "div.separator a";
      this.ongoing = /* @__PURE__ */ new Set(["on-going", "\u0111ang ti\u1EBFn h\xE0nh", "\u9032\u884C\u4E2D"]);
      this.finished = /* @__PURE__ */ new Set(["completed", "ho\xE0n th\xE0nh", "\u5B8C\u4E86"]);
      this.abandoned = /* @__PURE__ */ new Set(["canceled", "\u0111\xE3 hu\u1EF7 b\u1ECF", "\u30AD\u30E3\u30F3\u30BB\u30EB"]);
      this.paused = /* @__PURE__ */ new Set(["on-hold", "t\u1EA1m d\u1EEBng", "\u4E00\u6642\u505C\u6B62"]);
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    queryFirst(el, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const found = el.querySelector(selector);
          if (found)
            return found;
        } catch {
        }
      }
      return null;
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    sortParam(order) {
      switch (order) {
        case SortOrder.UPDATED:
          return "latest-updated";
        case SortOrder.POPULARITY:
          return "views";
        case SortOrder.ALPHABETICAL:
          return "az";
        case SortOrder.ALPHABETICAL_DESC:
          return "za";
        case SortOrder.NEWEST:
          return "new";
        case SortOrder.NEWEST_ASC:
          return "old";
        case SortOrder.RATING:
          return "score";
        default:
          return "latest-updated";
      }
    }
    stateParam(state) {
      switch (state) {
        case MangaState.ONGOING:
          return "on-going";
        case MangaState.FINISHED:
          return "completed";
        case MangaState.PAUSED:
          return "on-hold";
        case MangaState.ABANDONED:
          return "canceled";
        default:
          return "all";
      }
    }
    async getListPage(page, order, filter) {
      filter = filter || {};
      let url = `https://${this.domain}`;
      if (filter.query) {
        url += `${this.searchPath}${page}/?keyword=${encodeURIComponent(filter.query)}`;
      } else {
        url += `${this.filterPath}${page}/?sort=${this.sortParam(order)}`;
        const tags = (filter.tags || []).map((t) => t && t.key != null ? t.key : t).filter(Boolean);
        const tagsExclude = (filter.tagsExclude || []).map((t) => t && t.key != null ? t.key : t).filter(Boolean);
        url += `&genres=${tags.join(",")}`;
        url += `&notGenres=${tagsExclude.join(",")}`;
        const states = filter.states || [];
        if (states.length) {
          url += `&status=${this.stateParam(states[0])}`;
        }
      }
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, [
        this.selectMangaList,
        "div#main div.grid > div",
        "div.grid > div",
        "div.manga-lists div.grid > div"
      ]);
      const mangaList = [];
      const seen = /* @__PURE__ */ new Set();
      for (const el of elements) {
        const a = el.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href || href.startsWith("#"))
          continue;
        const relHref = this.toRelativeUrl(href);
        if (seen.has(relHref))
          continue;
        const titleEl = this.queryFirst(el, [this.selectMangaListTitle, ".text-center a", "h3 a", ".tooltip a", "a[title]"]);
        const img = el.querySelector("img");
        const title = (titleEl ? titleEl.textContent : a.getAttribute("title") || a.textContent || "").trim();
        if (!title)
          continue;
        seen.add(relHref);
        mangaList.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title,
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return mangaList;
    }
    parseState(text) {
      const t = (text || "").trim().toLowerCase();
      if (!t)
        return void 0;
      if (this.ongoing.has(t))
        return MangaState.ONGOING;
      if (this.finished.has(t))
        return MangaState.FINISHED;
      if (this.paused.has(t))
        return MangaState.PAUSED;
      if (this.abandoned.has(t))
        return MangaState.ABANDONED;
      return void 0;
    }
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const descEl = this.queryFirst(doc, [this.selectDescription, "div#syn-target", ".summary__content", ".description"]);
      const description = descEl ? descEl.innerHTML : manga.description || "";
      const coverEl = this.queryFirst(doc, [this.selectCover, ".a1 > figure img", ".a1 img", "figure img"]);
      const largeCoverUrl = coverEl ? this.imageSrc(coverEl) : manga.largeCoverUrl || manga.coverUrl;
      const tagEls = this.queryAll(doc, [this.selectTag, ".a2 div > a[rel='tag'].label", ".a2 a[rel='tag']"]);
      const tags = tagEls.map((a) => {
        const href = a.getAttribute("href") || "";
        const key = href.split("/").filter(Boolean).pop() || a.textContent.trim();
        return { title: a.textContent.trim(), key };
      }).filter((t) => t.title && !/^javascript:/i.test(t.key));
      const authorEl = this.queryFirst(doc, [this.selectAuthor, "div.y6x11p i.fas.fa-user + span.dt"]);
      const authorText = authorEl ? authorEl.textContent.trim() : "";
      const authors = authorText && authorText.toLowerCase() !== "updating" ? [authorText] : [];
      const stateEl = this.queryFirst(doc, [this.selectState, "div.y6x11p i.fas.fa-rss + span.dt"]);
      const state = this.parseState(stateEl ? stateEl.textContent : "");
      const chapterEls = this.queryAll(doc, [
        this.selectChapter,
        "ul > li.chapter",
        "li.chapter",
        "#chapterlist li",
        ".chapter-list li"
      ]).reverse();
      const chapters = chapterEls.map((el, i) => {
        const a = el.querySelector("a");
        if (!a)
          return null;
        const href = a.getAttribute("href");
        if (!href || href.startsWith("#"))
          return null;
        const relHref = this.toRelativeUrl(href);
        const timeEl = el.querySelector("time[datetime]");
        const ts = timeEl ? parseInt(timeEl.getAttribute("datetime"), 10) : NaN;
        const uploadDate = Number.isFinite(ts) ? ts * 1e3 : 0;
        return new MangaChapter({
          id: relHref,
          url: relHref,
          title: a.textContent.trim() || `Chapter ${i + 1}`,
          number: i + 1,
          volume: 0,
          uploadDate,
          source: this.source
        });
      }).filter((c) => c && c.url);
      return new Manga({
        ...manga,
        title: (this.queryFirst(doc, ["h1", ".manga-name", ".post-title"])?.textContent || manga.title || "").trim() || manga.title,
        description,
        coverUrl: manga.coverUrl || largeCoverUrl,
        largeCoverUrl,
        tags: tags.length ? tags : manga.tags,
        authors: authors.length ? authors : manga.authors,
        state: state || manga.state,
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        chapters
      });
    }
    extractChapterId(html, doc) {
      const marker = this.chapterIdMarker;
      let idx = html.indexOf(marker);
      if (idx !== -1) {
        const after = html.slice(idx + marker.length);
        const end = after.indexOf(";");
        const candidate = (end === -1 ? after : after.slice(0, end)).trim();
        const m = candidate.match(/\d+/);
        if (m)
          return m[0];
      }
      for (const script of Array.from(doc.querySelectorAll("script"))) {
        const data = script.textContent || "";
        const i = data.indexOf(marker);
        if (i !== -1) {
          const after = data.slice(i + marker.length);
          const end = after.indexOf(";");
          const candidate = (end === -1 ? after : after.slice(0, end)).trim();
          const m = candidate.match(/\d+/);
          if (m)
            return m[0];
        }
      }
      const loose = html.match(/CHAPTER_ID\s*=\s*["']?(\d+)/);
      return loose ? loose[1] : "";
    }
    parsePageHtml(html) {
      const doc = this.context.parseHTML(html);
      const anchorEls = this.queryAll(doc, [this.selectPageContainer, "div.separator a"]);
      const pages = [];
      if (anchorEls.length) {
        for (const a of anchorEls) {
          const url = a.getAttribute("href") || a.getAttribute("src") || "";
          if (!url)
            continue;
          const abs = this.toAbsoluteUrl(url);
          pages.push(new MangaPage({ id: abs, url: abs, source: this.source }));
        }
        if (pages.length)
          return pages;
      }
      const imgs = this.queryAll(doc, ["div img", "img"]);
      for (const img of imgs) {
        const url = img.getAttribute("src") || img.getAttribute("data-src") || "";
        if (!url)
          continue;
        const abs = this.toAbsoluteUrl(url);
        pages.push(new MangaPage({ id: abs, url: abs, source: this.source }));
      }
      return pages;
    }
    async getPages(chapter) {
      const fullUrl = this.toAbsoluteUrl(chapter.url);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      const chapterId = this.extractChapterId(html, doc);
      if (chapterId) {
        const ajaxUrl = `https://${this.domain}${this.ajaxImageListPath}${chapterId}`;
        try {
          const respText = await this.context.httpGet(ajaxUrl, this);
          const data = JSON.parse(respText);
          if (data && data.status === false) {
            throw new Error(data.msg || "Liliana ajax returned status=false");
          }
          if (data && typeof data.html === "string" && data.html.length) {
            const pages2 = this.parsePageHtml(data.html);
            if (pages2.length)
              return pages2;
          }
        } catch (e) {
          if (String(e.message || "").startsWith("Liliana ajax"))
            throw e;
        }
      }
      const inline = this.queryAll(doc, [
        "div.separator a",
        "div#chapter-content img",
        "div.reading-content img",
        "div#readerarea img",
        ".page-break img"
      ]);
      const pages = [];
      for (const el of inline) {
        const url = el.tagName === "A" ? el.getAttribute("href") || "" : el.getAttribute("src") || el.getAttribute("data-src") || "";
        if (!url || url.startsWith("data:"))
          continue;
        const abs = this.toAbsoluteUrl(url);
        pages.push(new MangaPage({ id: abs, url: abs, source: this.source }));
      }
      return pages;
    }
  };

  // src/madtheme.js
  var MadthemeParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 48) {
      super(context, source, domain, pageSize);
      this.listUrl = "search/";
      this.selectMangaList = "div.book-item";
      this.selectMangaListTitle = "div.meta div.title";
      this.selectDesc = "div.section-body.summary p.content";
      this.selectState = "div.detail p:contains(Status) span";
      this.selectAlt = "div.detail div.name h2";
      this.selectTag = "div.detail p:contains(Genres) a";
      this.selectChapter = "ul#chapter-list li";
      this.selectDate = ".chapter-update";
      this.selectChapterTitle = ".chapter-title";
      this.selectPage = "div#chapter-images img";
      this.imageSubDomain = null;
      this.imageFallbackHost = "sb.mbcdn.xyz";
      this.datePattern = "MMM dd, yyyy";
      this.ongoing = /* @__PURE__ */ new Set(["on going", "ongoing"]);
      this.finished = /* @__PURE__ */ new Set(["completed", "complete"]);
    }
    // --- helpers (mirrors madara.js conventions) ---------------------------
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    queryFirst(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const el = doc.querySelector(selector);
          if (el)
            return el;
        } catch {
        }
      }
      return null;
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // ":contains(...)" is a jsoup-ism the DOM rejects; resolve it manually so
    // overrides copied verbatim from the Kotlin still work.
    selectByContains(doc, selector) {
      const m = selector.match(/^(.*?):contains\(([^)]*)\)(.*)$/);
      if (!m) {
        return this.queryAll(doc, [selector]);
      }
      const [, head, needle, tail] = m;
      const headSel = head.trim();
      const needleLc = needle.trim().toLowerCase();
      const containers = headSel ? this.queryAll(doc, [headSel]) : Array.from(doc.querySelectorAll("*"));
      const matched = containers.filter((el) => (el.textContent || "").toLowerCase().includes(needleLc));
      if (!tail.trim())
        return matched;
      const tailTrim = tail.trim();
      const out = [];
      for (const el of matched) {
        if (tailTrim.startsWith("~")) {
          const sibSel = tailTrim.replace(/^~\s*/, "");
          let sib = el.nextElementSibling;
          while (sib) {
            try {
              if (sib.matches(sibSel))
                out.push(sib);
            } catch {
            }
            sib = sib.nextElementSibling;
          }
        } else {
          try {
            out.push(...Array.from(el.querySelectorAll(tailTrim)));
          } catch {
          }
        }
      }
      return out;
    }
    // --- list / search -----------------------------------------------------
    async getListPage(page, order, filter) {
      filter = filter || {};
      const listPath = this.listUrl.replace(/^\/+/, "").replace(/\/+$/, "");
      let url = `https://${this.domain}/${listPath}`;
      url += `?page=${page}`;
      if (filter.query) {
        url += `&q=${encodeURIComponent(filter.query)}`;
      }
      url += "&sort=";
      switch (order) {
        case SortOrder.POPULARITY:
          url += "views";
          break;
        case SortOrder.UPDATED:
          url += "updated_at";
          break;
        case SortOrder.ALPHABETICAL:
          url += "name";
          break;
        case SortOrder.NEWEST:
          url += "created_at";
          break;
        case SortOrder.RATING:
          url += "rating";
          break;
        default:
          url += "updated_at";
          break;
      }
      const tags = filter.tags || [];
      if (tags.length) {
        for (const t of tags) {
          const key = t && (t.key || t);
          if (key)
            url += `&genre[]=${encodeURIComponent(key)}`;
        }
      }
      const states = filter.states || [];
      const state = Array.isArray(states) ? states[0] : states;
      if (state) {
        let s = "all";
        if (state === MangaState.ONGOING)
          s = "ongoing";
        else if (state === MangaState.FINISHED)
          s = "completed";
        url += `&status=${s}`;
      }
      const html = await this.context.httpGet(url, this);
      const nextData = this.parseNextData(html);
      if (nextData) {
        const list = this.parseNextList(nextData);
        if (list.length)
          return list;
      }
      return this.parseMangaList(html);
    }
    // --- Next.js (mangak.io) JSON extraction --------------------------------
    parseNextData(html) {
      const m = html.match(/<script[^>]*id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/) || html.match(/<script[^>]*type="application\/json"[^>]*>([\s\S]*?)<\/script>/);
      if (!m)
        return null;
      try {
        const data = JSON.parse(m[1]);
        return data && data.props && data.props.pageProps ? data.props.pageProps : null;
      } catch {
        return null;
      }
    }
    nextItemToManga(item) {
      if (!item || !item.url)
        return null;
      const href = this.toRelativeUrl(item.url);
      const genres = Array.isArray(item.genres) ? item.genres : [];
      const status = String(item.status || "").toLowerCase();
      let state;
      if (this.ongoing.has(status) || status === "ongoing")
        state = MangaState.ONGOING;
      else if (this.finished.has(status) || status === "completed")
        state = MangaState.FINISHED;
      const isAdult = item.isAdult === true || String(item.contentRating || "").toLowerCase() === "adult";
      return new Manga({
        id: href,
        url: href,
        publicUrl: this.toAbsoluteUrl(href),
        coverUrl: item.cover || "",
        largeCoverUrl: item.cover || "",
        title: item.name || item.displayAltName || "",
        altTitles: (item.altNames || []).map((a) => a && a.name).filter(Boolean),
        rating: typeof item.rating === "number" ? item.rating / 5 : 0,
        tags: genres.map((g) => ({ key: g.slug || g.name, title: g.name })).filter((t) => t.key || t.title),
        state,
        description: item.summary || "",
        source: this.source,
        contentRating: isAdult || this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
      });
    }
    parseNextList(pp) {
      const items = pp.ssrItems || pp.items || pp.mangas || [];
      if (!Array.isArray(items))
        return [];
      return items.map((it) => this.nextItemToManga(it)).filter(Boolean);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, [
        this.selectMangaList,
        "div.book-item",
        "div.book-detailed-item"
      ]);
      const out = [];
      for (const div of elements) {
        const a = div.querySelector("a[href]");
        if (!a)
          continue;
        const href = this.toRelativeUrl(a.getAttribute("href"));
        if (!href || href.includes("/chapter"))
          continue;
        const titleEl = this.queryFirst(div, [
          this.selectMangaListTitle,
          "div.meta div.title",
          "div.title",
          ".title"
        ]);
        const title = (titleEl ? titleEl.textContent : a.getAttribute("title") || a.textContent || "").trim();
        const img = div.querySelector("img");
        const scoreEl = div.querySelector("div.meta span.score, span.score");
        let rating = 0;
        if (scoreEl) {
          const v = parseFloat((scoreEl.textContent || "").trim());
          if (!Number.isNaN(v))
            rating = v / 5;
        }
        out.push(new Manga({
          id: href,
          url: href,
          publicUrl: this.toAbsoluteUrl(href),
          coverUrl: this.imageSrc(img),
          title,
          rating,
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return out;
    }
    // --- details -----------------------------------------------------------
    async getDetails(manga) {
      const fullUrl = this.toAbsoluteUrl(manga.url);
      const html = await this.context.httpGet(fullUrl, this);
      const nextData = this.parseNextData(html);
      if (nextData && nextData.initialManga) {
        return this.parseNextDetails(manga, nextData.initialManga);
      }
      const doc = this.context.parseHTML(html);
      const descEl = this.queryFirst(doc, [this.selectDesc, ".summary .content", "div.section-body.summary p.content", ".summary p"]);
      const description = descEl ? descEl.innerHTML : "";
      const stateEl = this.selectByContains(doc, this.selectState)[0] || this.selectByContains(doc, ".detail .meta > p > strong:contains(Status) ~ a")[0];
      let state = void 0;
      if (stateEl) {
        const t = (stateEl.textContent || "").trim().toLowerCase();
        if (this.ongoing.has(t))
          state = MangaState.ONGOING;
        else if (this.finished.has(t))
          state = MangaState.FINISHED;
      }
      const altEl = this.queryFirst(doc, [this.selectAlt, ".detail h2", "div.detail div.name h2"]);
      const altText = altEl ? (altEl.textContent || "").trim() : "";
      const altTitles = altText ? altText.split(/[,;]/).map((s) => s.trim()).filter(Boolean) : [];
      const tagEls = this.selectByContains(doc, this.selectTag);
      const tags = tagEls.map((a) => {
        const href = a.getAttribute && a.getAttribute("href") || "";
        const key = href.replace(/\/$/, "").split("/").pop() || "";
        return { key, title: (a.textContent || "").replace(/,/g, "").trim() };
      }).filter((t) => t.key || t.title);
      const nsfw = !!doc.getElementById("adt-warning");
      const titleEl = this.queryFirst(doc, ["h1", ".detail h1"]);
      const title = titleEl ? (titleEl.textContent || "").trim() : manga.title;
      const coverEl = this.queryFirst(doc, ["#cover img", ".detail .img-cover img", ".book-info img"]);
      const cover = coverEl ? this.imageSrc(coverEl) : manga.coverUrl;
      const chapters = await this.getChapters(doc, manga);
      return new Manga({
        ...manga,
        title,
        description,
        altTitles: altTitles.length ? altTitles : manga.altTitles,
        tags: tags.length ? tags : manga.tags,
        state,
        coverUrl: cover || manga.coverUrl,
        largeCoverUrl: cover || manga.largeCoverUrl || manga.coverUrl,
        contentRating: nsfw || this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        source: this.source,
        chapters
      });
    }
    parseNextDetails(manga, im) {
      const genres = Array.isArray(im.genres) ? im.genres : [];
      const tags = genres.map((g) => ({ key: g.slug || g.name, title: g.name })).filter((t) => t.key || t.title);
      const status = String(im.status || "").toLowerCase();
      let state;
      if (this.ongoing.has(status) || status === "ongoing")
        state = MangaState.ONGOING;
      else if (this.finished.has(status) || status === "completed")
        state = MangaState.FINISHED;
      const authors = [].concat(im.authors || [], im.artists || []).map((a) => typeof a === "string" ? a : a && (a.name || a.slug)).filter(Boolean);
      const isAdult = im.isAdult === true || String(im.contentRating || "").toLowerCase() === "adult";
      const rawChapters = Array.isArray(im.chapters) ? im.chapters.slice().reverse() : [];
      const chapters = rawChapters.map((c, i) => {
        const href = this.toRelativeUrl(c.url || "");
        if (!href)
          return null;
        const num = parseFloat(c.chapterNumber);
        const date = c.updatedAt || c.date;
        return new MangaChapter({
          id: href,
          url: href,
          title: c.name || c.slug || `Chapter ${i + 1}`,
          number: Number.isNaN(num) ? i + 1 : num,
          volume: 0,
          uploadDate: date ? Date.parse(date) || 0 : 0,
          source: this.source
        });
      }).filter(Boolean);
      return new Manga({
        ...manga,
        title: im.name || manga.title,
        description: im.summary || manga.description,
        altTitles: (im.altNames || []).map((a) => a && a.name).filter(Boolean),
        tags: tags.length ? tags : manga.tags,
        authors: authors.length ? authors : manga.authors,
        state,
        coverUrl: im.cover || manga.coverUrl,
        largeCoverUrl: im.cover || manga.largeCoverUrl || manga.coverUrl,
        rating: typeof im.rating === "number" ? im.rating / 5 : manga.rating || 0,
        contentRating: isAdult || this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        source: this.source,
        chapters
      });
    }
    /**
     * Resolve the chapter-list HTML. Three known shapes:
     *  - MangaJinx: bookId -> /service/backend/chaplist/?manga_id=<id>
     *  - base/MadthemeParser: bookSlug -> /api/manga/<slug>/chapters?source=detail
     *  - ManhuaScan: chapters already inline in the detail document
     * We try the inline doc first, then bookId, then bookSlug — using whichever
     * yields chapters, so per-source endpoint differences self-heal.
     */
    async getChapters(doc, manga) {
      let chapters = this.parseChapterList(doc);
      if (chapters.length)
        return chapters;
      const detailHtml = doc.documentElement ? doc.documentElement.outerHTML : "";
      const bookId = (detailHtml.match(/bookId\s*=\s*(\d+)/) || [])[1];
      const bookSlug = (detailHtml.match(/bookSlug\s*=\s*["']([^"']+)["']/) || [])[1];
      const endpoints = [];
      if (bookId)
        endpoints.push(`https://${this.domain}/service/backend/chaplist/?manga_id=${bookId}`);
      if (bookSlug)
        endpoints.push(`https://${this.domain}/api/manga/${bookSlug}/chapters?source=detail`);
      for (const url of endpoints) {
        try {
          const html = await this.context.httpGet(url, this);
          const cdoc = this.context.parseHTML(html);
          chapters = this.parseChapterList(cdoc);
          if (chapters.length)
            return chapters;
        } catch {
        }
      }
      return chapters;
    }
    parseChapterList(doc) {
      const elements = this.queryAll(doc, [
        this.selectChapter,
        "ul#chapter-list li",
        "#chapter-list > li",
        "#chapter-list-inner .chapter-list > li",
        "ul.chapter-list li",
        ".chapter-list li"
      ]);
      if (!elements.length)
        return [];
      const reversed = elements.slice().reverse();
      const out = [];
      for (let i = 0; i < reversed.length; i++) {
        const li = reversed[i];
        const a = li.querySelector("a[href]");
        if (!a)
          continue;
        const href = this.toRelativeUrl(a.getAttribute("href"));
        if (!href || href.includes("#"))
          continue;
        const titleEl = this.queryFirst(li, [this.selectChapterTitle, ".chapter-title"]);
        const title = (titleEl ? titleEl.textContent : a.getAttribute("title") || a.textContent || "").replace(/\s+/g, " ").trim();
        const dateEl = this.queryFirst(li, [this.selectDate, ".chapter-update"]);
        const dateText = dateEl ? (dateEl.textContent || "").trim() : "";
        out.push(new MangaChapter({
          id: href,
          url: href,
          title: title || `Chapter ${i + 1}`,
          number: i + 1,
          volume: 0,
          uploadDate: this.parseChapterDate(dateText),
          source: this.source
        }));
      }
      return out;
    }
    // --- pages -------------------------------------------------------------
    async getPages(chapter) {
      const fullUrl = this.normalizedChapterUrl(chapter.url);
      const html = await this.context.httpGet(fullUrl, this);
      const nextData = this.parseNextData(html);
      if (nextData && nextData.initialChapter && Array.isArray(nextData.initialChapter.images)) {
        const imgs = nextData.initialChapter.images;
        const seen = /* @__PURE__ */ new Set();
        const out = [];
        for (const u of imgs) {
          const url = (u || "").trim();
          if (!url || seen.has(url))
            continue;
          seen.add(url);
          out.push(new MangaPage({ id: url, url, source: this.source }));
        }
        if (out.length)
          return out;
      }
      const doc = this.context.parseHTML(html);
      const known = /* @__PURE__ */ new Set();
      const result = [];
      const addPage = (rawUrl) => {
        if (!rawUrl)
          return;
        const url = this.resolveChapterImageUrl(rawUrl);
        if (!url || known.has(url))
          return;
        known.add(url);
        result.push(new MangaPage({ id: url, url, source: this.source }));
      };
      for (const img of this.queryAll(doc, [this.selectPage, "div#chapter-images img", "#chapter-images img"])) {
        addPage(this.resolveImageElementUrl(img));
      }
      const scripts = Array.from(doc.querySelectorAll("script")).map((s) => s.textContent || s.innerHTML || "");
      let mainServer = null;
      for (const s of scripts) {
        const m = s.match(/mainServer\s*=\s*"(.*?)"/);
        if (m) {
          mainServer = m[1];
          break;
        }
      }
      if (!mainServer) {
        const m = html.match(/mainServer\s*=\s*"(.*?)"/);
        if (m)
          mainServer = m[1];
      }
      const schemePrefix = mainServer && mainServer.startsWith("//") ? "https:" : "";
      let chapImagesRaw = null;
      for (const s of scripts) {
        const m = s.match(/chapImages\s*=\s*['"](.*?)['"]/s);
        if (m) {
          chapImagesRaw = m[1];
          break;
        }
      }
      if (!chapImagesRaw) {
        const m = html.match(/chapImages\s*=\s*['"](.*?)['"]/s);
        if (m)
          chapImagesRaw = m[1];
      }
      if (chapImagesRaw) {
        for (const piece of chapImagesRaw.split(",")) {
          const u = piece.trim();
          if (!u)
            continue;
          if (mainServer) {
            addPage(`${schemePrefix}${mainServer}${u}`);
          } else {
            addPage(u);
          }
        }
      }
      return result;
    }
    // <img> with optional onerror="this.src='...'" fallback (base template).
    resolveImageElementUrl(img) {
      const primary = this.imageSrc(img);
      const onerror = img.getAttribute && img.getAttribute("onerror") || "";
      const m = onerror.match(/this\.src='([^']*)'/);
      if (!m || !m[1])
        return primary;
      const fallback = this.resolveChapterImageUrl(m[1]);
      if (!/^https?:\/\//.test(fallback))
        return primary;
      return primary.includes("://s20.") ? fallback : primary;
    }
    resolveChapterImageUrl(rawUrl) {
      const value = (rawUrl || "").trim();
      if (!value)
        return "";
      if (value.startsWith("https://") || value.startsWith("http://")) {
        return this.applyImageSubDomain(value);
      }
      if (value.startsWith("//"))
        return this.applyImageSubDomain(`https:${value}`);
      if (value.includes("/manga/") && !value.includes("/wp-content/")) {
        const host = this.imageSubDomain || this.imageFallbackHost;
        return `https://${host}/manga${value.substring(value.indexOf("/manga") + "/manga".length)}`;
      }
      if (value.startsWith("/"))
        return `https://${this.domain}${value}`;
      return this.toAbsoluteUrl(value);
    }
    // When a per-source imageSubDomain is configured (MangaXyz/Puma/Cute/Forest),
    // rewrite absolute chapImages URLs onto it, preserving everything after /manga.
    applyImageSubDomain(absUrl) {
      if (!this.imageSubDomain)
        return absUrl;
      const idx = absUrl.indexOf("/manga");
      if (idx === -1)
        return absUrl;
      return `https://${this.imageSubDomain}/manga${absUrl.substring(idx + "/manga".length)}`;
    }
    normalizedChapterUrl(url) {
      const value = (url || "").trim();
      if (value.startsWith("https://") || value.startsWith("http://")) {
        const schemeEnd = value.indexOf("://");
        const pathStart = value.indexOf("/", schemeEnd + 3);
        if (pathStart === -1)
          return value;
        const prefix = value.substring(0, pathStart);
        const path = value.substring(pathStart).replace(/\/{2,}/g, "/");
        return prefix + path;
      }
      return this.toAbsoluteUrl(value);
    }
    // --- dates -------------------------------------------------------------
    parseChapterDate(date) {
      if (!date)
        return 0;
      const d = date.toLowerCase().trim();
      if (/\bago\b/.test(d) || /\b\d+\s*[hd]\b/.test(d)) {
        return this.parseRelativeDate(d);
      }
      if (d.startsWith("today")) {
        const now = /* @__PURE__ */ new Date();
        now.setHours(0, 0, 0, 0);
        return now.getTime();
      }
      const t = Date.parse(date.replace(/(\d+)(st|nd|rd|th)/gi, "$1"));
      return Number.isNaN(t) ? 0 : t;
    }
    parseRelativeDate(date) {
      const num = parseInt((date.match(/(\d+)/) || [])[1], 10);
      if (Number.isNaN(num))
        return 0;
      const now = /* @__PURE__ */ new Date();
      if (/\bsecond/.test(date))
        now.setSeconds(now.getSeconds() - num);
      else if (/\bmin/.test(date))
        now.setMinutes(now.getMinutes() - num);
      else if (/\bhour/.test(date) || /\b\d+\s*h\b/.test(date))
        now.setHours(now.getHours() - num);
      else if (/\bday/.test(date) || /\b\d+\s*d\b/.test(date))
        now.setDate(now.getDate() - num);
      else if (/\bmonth/.test(date))
        now.setMonth(now.getMonth() - num);
      else if (/\byear/.test(date))
        now.setFullYear(now.getFullYear() - num);
      else
        return 0;
      return now.getTime();
    }
  };

  // src/scan.js
  var ScanParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 0) {
      super(context, source, domain, pageSize);
      this.listUrl = "/manga";
      this.datePattern = "MM-dd-yyyy";
      this.selectMangaList = ".series, .series-paginated .grid-item-series";
      this.selectMangaListTitle = ".link-series h3, .item-title";
      this.selectRating = ".card-series-detail .rate-value span, .card-series-about .rate-value span";
      this.selectAuthor = ".card-series-detail .col-6:contains(Autore) div, .card-series-about .mb-3:contains(Autore) a";
      this.selectAltTitle = ".card div.col-12.mb-4 h2, .card-series-about .h6";
      this.selectDescription = ".card div.col-12.mb-4 p, .card-series-desc .mb-4 p";
      this.selectChapter = ".chapters-list .col-chapter, .card-list-chapter .col-chapter";
      this.selectPage = ".book-page .img-fluid";
      this.maxPages = 600;
      this.sortQueryMap = {
        [SortOrder.UPDATED]: "u",
        [SortOrder.ALPHABETICAL]: "a",
        [SortOrder.POPULARITY]: "p",
        [SortOrder.RATING]: "r"
      };
    }
    // queryAll fallback helper (matches madara.js / mangareader.js convention):
    // try each selector in turn, return the first non-empty match.
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    querySelector(el, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const found = el.querySelector(selector);
          if (found)
            return found;
        } catch {
        }
      }
      return null;
    }
    // Kotlin reads cover from img[data-src] and strips tab chars.
    imageSrc(img) {
      if (!img)
        return "";
      const url = (img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "").replace(/\t/g, "").trim();
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // Decode the JSON-escaped HTML fragment the /search endpoint returns.
    // Kotlin: Jsoup.parseBodyFragment(raw.unescapeJson(), domain).
    unescapeJson(raw) {
      if (!raw)
        return "";
      let s = raw.trim();
      if (s.startsWith('"') && s.endsWith('"'))
        s = s.slice(1, -1);
      return s.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\n/g, "\n").replace(/\\r/g, "\r").replace(/\\t/g, "	").replace(/\\\//g, "/").replace(/\\"/g, '"').replace(/\\\\/g, "\\");
    }
    sortValue(order) {
      return this.sortQueryMap[order] || "u";
    }
    async getListPage(page, order, filter) {
      filter = filter || {};
      const query = filter.query;
      let isSearch = false;
      let url;
      if (query) {
        url = `https://${this.domain}/search?q=${encodeURIComponent(query)}`;
        isSearch = true;
      } else {
        let u = `https://${this.domain}${this.listUrl}?q=${this.sortValue(order)}`;
        const tags = filter.tags || [];
        for (const tag of tags) {
          const key = tag && (tag.key !== void 0 ? tag.key : tag);
          if (key)
            u += `&search[tags][]=${encodeURIComponent(key)}`;
        }
        u += `&page=${page}`;
        url = u;
      }
      const raw = await this.context.httpGet(url, this);
      const html = isSearch ? this.unescapeJson(raw) : raw;
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, [
        this.selectMangaList,
        ".series",
        ".series-paginated .grid-item-series",
        ".grid-item-series"
      ]);
      const list = [];
      for (const div of elements) {
        const a = div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const img = div.querySelector("img");
        const titleEl = this.querySelector(div, [this.selectMangaListTitle, ".link-series h3", ".item-title", "h3"]);
        const title = (titleEl ? titleEl.textContent : a.getAttribute("title") || a.textContent || "").trim();
        list.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title,
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return list;
    }
    // Kotlin chapter title: h5.html().substringBefore("<div").substringAfter("</span>").
    // The <h5> looks like: <span>...</span>Chapter title<div>date</div>.
    extractChapterTitle(h5) {
      if (!h5)
        return null;
      let html = h5.innerHTML || "";
      const beforeDiv = html.split("<div")[0];
      const afterSpan = beforeDiv.includes("</span>") ? beforeDiv.slice(beforeDiv.indexOf("</span>") + "</span>".length) : beforeDiv;
      const text = afterSpan.replace(/<[^>]*>/g, "").replace(/\s+/g, " ").trim();
      return text || null;
    }
    // Parse "MM-dd-yyyy" -> epoch millis (0 if unparseable), mirroring parseSafe.
    parseDate(text) {
      if (!text)
        return 0;
      const m = text.trim().match(/(\d{1,2})-(\d{1,2})-(\d{4})/);
      if (!m)
        return 0;
      const month = parseInt(m[1], 10) - 1;
      const day = parseInt(m[2], 10);
      const year = parseInt(m[3], 10);
      const d = new Date(year, month, day);
      return isNaN(d.getTime()) ? 0 : d.getTime();
    }
    parseChapters(doc) {
      const elements = this.queryAll(doc, [
        this.selectChapter,
        ".chapters-list .col-chapter",
        ".card-list-chapter .col-chapter"
      ]);
      const reversed = elements.slice().reverse();
      const chapters = [];
      for (let i = 0; i < reversed.length; i++) {
        const div = reversed[i];
        const a = div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const h5 = div.querySelector("h5");
        const dateEl = div.querySelector("h5 div") || doc.querySelector("h5 div");
        chapters.push(new MangaChapter({
          id: relHref,
          url: relHref,
          title: this.extractChapterTitle(h5),
          number: i + 1,
          volume: 0,
          branch: null,
          scanlator: null,
          uploadDate: this.parseDate(dateEl ? dateEl.textContent : ""),
          source: this.source
        }));
      }
      return chapters;
    }
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const ratingEl = this.querySelector(doc, [this.selectRating, ".rate-value span"]);
      let rating = 0;
      if (ratingEl) {
        const ownText = ratingEl.childNodes[0] && ratingEl.childNodes[0].nodeType === 3 ? ratingEl.childNodes[0].textContent : ratingEl.textContent;
        const r = parseFloat((ownText || "").trim());
        if (!isNaN(r))
          rating = r / 5;
      }
      const authorEl = this.querySelector(doc, [
        this.selectAuthor,
        ".card-series-detail .col-6:contains(Autore) div",
        ".card-series-about .mb-3:contains(Autore) a"
      ]);
      const author = authorEl ? authorEl.textContent.trim() : null;
      const altEl = this.querySelector(doc, [this.selectAltTitle, ".card div.col-12.mb-4 h2", ".card-series-about .h6"]);
      const altTitle = altEl ? altEl.textContent.trim() : null;
      const descEl = this.querySelector(doc, [this.selectDescription, ".card div.col-12.mb-4 p", ".card-series-desc .mb-4 p"]);
      const description = descEl ? descEl.innerHTML : manga.description || "";
      let chapters = this.parseChapters(doc);
      if (!chapters.length) {
        try {
          const btn = doc.querySelector(".container-fluid button.w-100[data-path], button[data-path*='/books']");
          const dataPath = btn ? btn.getAttribute("data-path") : null;
          if (dataPath && dataPath.includes("/manga/")) {
            const id = dataPath.split("/manga/")[1].split("/books")[0];
            if (id) {
              const booksHtml = await this.context.httpGet(`https://${this.domain}/manga/${id}/books`, this);
              const booksDoc = this.context.parseHTML(booksHtml);
              chapters = this.parseChapters(booksDoc);
            }
          }
        } catch {
        }
      }
      return new Manga({
        ...manga,
        rating: rating || manga.rating || 0,
        authors: author ? [author] : manga.authors || [],
        altTitles: altTitle ? [altTitle] : manga.altTitles || [],
        description,
        contentRating: this.source.isNsfw ? ContentRating.ADULT : manga.contentRating || ContentRating.SAFE,
        source: this.source,
        chapters
      });
    }
    async getPages(chapter) {
      const fullUrl = this.toAbsoluteUrl(chapter.url).replace(/\/$/, "");
      const pages = [];
      for (let n = 1; n <= this.maxPages; n++) {
        let html;
        try {
          html = await this.context.httpGet(`${fullUrl}/${n}`, this);
        } catch {
          break;
        }
        const doc = this.context.parseHTML(html);
        const img = this.querySelector(doc, [this.selectPage, ".book-page .img-fluid", ".book-page img"]);
        const src = img ? this.imageSrc(img) : "";
        if (!src)
          break;
        pages.push(new MangaPage({
          id: src,
          url: src,
          source: this.source
        }));
      }
      return pages;
    }
  };

  // src/iken.js
  var IkenParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 18) {
      super(context, source, domain, pageSize);
      this.useApi = true;
      this.apiDomainOverride = null;
      this.datePattern = "yyyy-MM-dd";
      this.selectPages = "main section img";
      this.selectLock = "svg.lucide-lock";
      this.queryPath = "/api/query";
      this.chaptersPath = "/api/chapters";
      this.chapterPath = "/api/chapter";
      this.seriesPathPrefix = "/series/";
      this.listPerPage = 18;
    }
    get apiDomain() {
      if (this.apiDomainOverride)
        return this.apiDomainOverride;
      return this.useApi ? `api.${this.domain}` : this.domain;
    }
    apiHeaders() {
      return {
        "Accept": "application/json, text/plain, */*",
        "Origin": `https://${this.domain}`,
        "Referer": `https://${this.domain}/`
      };
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    async getJson(url, headers) {
      const text = await this.context.httpGet(url, this);
      return JSON.parse(text);
    }
    mapState(status) {
      switch (String(status || "").toUpperCase()) {
        case "ONGOING":
        case "MASS_RELEASED":
          return MangaState.ONGOING;
        case "COMPLETED":
          return MangaState.FINISHED;
        case "DROPPED":
        case "CANCELLED":
          return MangaState.ABANDONED;
        case "COMING_SOON":
          return MangaState.UPCOMING;
        case "HIATUS":
          return MangaState.PAUSED;
        default:
          return void 0;
      }
    }
    mapSeriesType(order, filter) {
      const states = filter && filter.states || [];
      const first = Array.isArray(states) ? states[0] : states;
      switch (first) {
        case MangaState.ONGOING:
          return "ONGOING";
        case MangaState.FINISHED:
          return "COMPLETED";
        case MangaState.UPCOMING:
          return "COMING_SOON";
        case MangaState.ABANDONED:
          return "DROPPED";
        default:
          return "";
      }
    }
    // ---- List ----------------------------------------------------------
    async getListPage(page, order, filter) {
      filter = filter || {};
      const perPage = this.pageSize || this.listPerPage || 18;
      let url = `https://${this.apiDomain}${this.queryPath}?page=${page}&perPage=${perPage}&searchTerm=`;
      if (filter.query)
        url += encodeURIComponent(filter.query);
      if (filter.tags && filter.tags.length) {
        const ids = filter.tags.map((t) => t && (t.key != null ? t.key : t)).filter((k) => k != null);
        if (ids.length)
          url += `&genreIds=${ids.join(",")}`;
      }
      url += `&seriesType=`;
      url += `&seriesStatus=${this.mapSeriesType(order, filter)}`;
      const json = await this.getJson(url, this.apiHeaders());
      return this.parseMangaList(json);
    }
    parseMangaList(json) {
      const posts = json && Array.isArray(json.posts) ? json.posts : [];
      const out = [];
      for (const it of posts) {
        const slug = it.slug;
        if (!slug)
          continue;
        const url = `${this.seriesPathPrefix}${slug}`;
        const isNsfwSource = it.hot === true || this.source.isNsfw === true;
        const author = it.author && String(it.author).trim() || null;
        const description = it.postContent || it.description || "";
        out.push(new Manga({
          id: it.id != null ? it.id : url,
          url,
          publicUrl: this.toAbsoluteUrl(url),
          coverUrl: it.featuredImage || "",
          title: it.postTitle || it.title || "",
          altTitles: it.alternativeTitles ? [it.alternativeTitles] : [],
          description,
          rating: 0,
          tags: [],
          authors: author ? [author] : [],
          state: this.mapState(it.seriesStatus),
          source: this.source,
          contentRating: isNsfwSource ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return out;
    }
    // ---- Details / Chapters --------------------------------------------
    seriesIdFromManga(manga) {
      if (manga.id != null && /^\d+$/.test(String(manga.id)))
        return String(manga.id);
      return null;
    }
    slugFromUrl(url) {
      const rel = this.toRelativeUrl(url || "");
      const m = rel.match(/\/series\/([^/?#]+)/);
      return m ? m[1] : "";
    }
    async getDetails(manga) {
      let seriesId = this.seriesIdFromManga(manga);
      const slugFromManga = this.slugFromUrl(manga.url);
      let queryPost = null;
      if (!seriesId && slugFromManga) {
        queryPost = await this.findPostBySlug(slugFromManga);
        seriesId = queryPost && queryPost.id != null ? String(queryPost.id) : null;
      }
      if (!seriesId)
        throw new Error("Unable to resolve series id for details");
      const url = `https://${this.apiDomain}${this.chaptersPath}?postId=${seriesId}&skip=0&take=900&order=desc&userid=`;
      const json = await this.getJson(url, this.apiHeaders());
      const post = json && json.post || {};
      const slug = post.slug || slugFromManga || "";
      const data = Array.isArray(post.chapters) ? post.chapters : [];
      const ordered = data.slice().reverse();
      const chapters = ordered.map((it, i) => {
        const slugName = slug && String(slug) || it.mangaPost && it.mangaPost.slug || "";
        const chapterUrl = `${this.seriesPathPrefix}${slugName}/${it.slug}`;
        const number = Number(it.number) || 0;
        const extra = it.title && String(it.title).trim() ? ` - ${String(it.title).trim()}` : "";
        return new MangaChapter({
          id: it.id != null ? it.id : chapterUrl,
          url: chapterUrl,
          title: `Chapter ${this.formatNumber(number)}${extra}`,
          number,
          volume: 0,
          branch: null,
          scanlator: null,
          uploadDate: it.createdAt ? new Date(it.createdAt).getTime() : 0,
          source: this.source,
          index: i
        });
      });
      const meta = post;
      const qp = queryPost || {};
      const genres = Array.isArray(meta.genres) ? meta.genres : Array.isArray(qp.genres) ? qp.genres : [];
      return new Manga({
        ...manga,
        title: meta.postTitle || qp.postTitle || manga.title,
        coverUrl: meta.featuredImage || qp.featuredImage || manga.coverUrl || "",
        largeCoverUrl: meta.featuredImage || qp.featuredImage || manga.largeCoverUrl || manga.coverUrl || "",
        description: meta.postContent || meta.description || qp.postContent || manga.description || "",
        tags: genres.map((g) => ({ title: g.name || g.title || String(g), key: String(g.id != null ? g.id : g.name || g) })),
        state: this.mapState(meta.seriesStatus) || this.mapState(qp.seriesStatus) || manga.state,
        chapters
      });
    }
    formatNumber(n) {
      if (Number.isInteger(n))
        return String(n);
      return String(n);
    }
    // Resolve a series slug to its full query post (carries id, genres, cover).
    // The API searches by title text, not by slug, so a raw hyphenated slug
    // (especially one with an apostrophe, e.g. "...female-lead's-sister...")
    // returns nothing. Search with a cleaned title-like term first, then fall
    // back to the raw slug. Always prefer an exact slug match in the results.
    async findPostBySlug(seriesSlug) {
      const cleaned = String(seriesSlug).replace(/[^a-zA-Z0-9]+/g, " ").trim();
      const terms = cleaned && cleaned !== seriesSlug ? [cleaned, seriesSlug] : [seriesSlug];
      for (const term of terms) {
        try {
          const json = await this.getJson(
            `https://${this.apiDomain}${this.queryPath}?page=1&perPage=20&searchTerm=${encodeURIComponent(term)}`,
            this.apiHeaders()
          );
          const posts = json && Array.isArray(json.posts) ? json.posts : [];
          if (!posts.length)
            continue;
          const exact = posts.find((p) => p.slug === seriesSlug);
          const hit = exact || posts[0];
          if (hit && hit.id != null)
            return hit;
        } catch {
        }
      }
      return null;
    }
    async findPostIdBySlug(seriesSlug) {
      const post = await this.findPostBySlug(seriesSlug);
      return post && post.id != null ? String(post.id) : null;
    }
    // ---- Pages ---------------------------------------------------------
    async getPages(chapter) {
      if (this.useApi) {
        try {
          const apiPages = await this.readChapterImages(chapter.id);
          if (apiPages && apiPages.length)
            return apiPages;
        } catch (e) {
          if (String(e && e.message).toLowerCase().includes("unlock"))
            throw e;
        }
        const numericId = /^\d+$/.test(String(chapter.id)) ? null : await this.resolveChapterId(chapter);
        if (numericId) {
          try {
            const apiPages = await this.readChapterImages(numericId);
            if (apiPages && apiPages.length)
              return apiPages;
          } catch (e) {
            if (String(e && e.message).toLowerCase().includes("unlock"))
              throw e;
          }
        }
      }
      const fullUrl = this.toAbsoluteUrl(chapter.url);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      if (doc.querySelector(this.selectLock)) {
        throw new Error("Need to unlock chapter!");
      }
      const fromNext = this.extractNextImages(html);
      if (fromNext.length) {
        return fromNext.map((u) => new MangaPage({ id: u, url: u, source: this.source }));
      }
      const imgs = this.queryAll(doc, [this.selectPages, "main section img", "section img", "img"]);
      const urls = [];
      const seen = /* @__PURE__ */ new Set();
      for (const img of imgs) {
        const u = this.imageSrc(img);
        if (!u || u.startsWith("data:") || seen.has(u))
          continue;
        if (/\/(logo|avatar|icon|banner)\b/i.test(u))
          continue;
        seen.add(u);
        urls.push(u);
      }
      return urls.map((u) => new MangaPage({ id: u, url: u, source: this.source }));
    }
    async resolveChapterId(chapter) {
      const rel = this.toRelativeUrl(chapter.url).split("?")[0];
      const parts = rel.replace(/^\/+|\/+$/g, "").split("/");
      if (parts.length < 3 || parts[0] !== "series")
        return null;
      const seriesSlug = parts[1];
      const chapterSlug = parts[2];
      const postId = await this.findPostIdBySlug(seriesSlug);
      if (!postId)
        return null;
      try {
        const json = await this.getJson(
          `https://${this.apiDomain}${this.chaptersPath}?postId=${postId}&skip=0&take=900&order=desc&userid=`,
          this.apiHeaders()
        );
        const list = json && json.post && Array.isArray(json.post.chapters) ? json.post.chapters : [];
        const hit = list.find((c) => c.slug === chapterSlug);
        return hit && hit.id != null && Number(hit.id) > 0 ? String(hit.id) : null;
      } catch {
        return null;
      }
    }
    async readChapterImages(chapterId) {
      if (chapterId == null || !/^\d+$/.test(String(chapterId)) || Number(chapterId) <= 0)
        return [];
      const json = await this.getJson(
        `https://${this.apiDomain}${this.chapterPath}?chapterId=${chapterId}`,
        this.apiHeaders()
      );
      const chapterJson = json && json.chapter;
      if (!chapterJson)
        return [];
      if (chapterJson.isLocked === true || chapterJson.isAccessible === false) {
        throw new Error("Need to unlock chapter!");
      }
      const images = Array.isArray(chapterJson.images) ? chapterJson.images : [];
      const pages = images.map((item) => {
        if (!item)
          return null;
        const url = item.url || item.src || item.image;
        if (!url)
          return null;
        return {
          order: this.parseOrder(item.order),
          url: String(url).replace("/public//", "/public/")
        };
      }).filter(Boolean).sort((a, b) => a.order - b.order);
      return pages.map((p) => new MangaPage({ id: p.url, url: p.url, source: this.source }));
    }
    parseOrder(v) {
      const n = parseInt(v, 10);
      return Number.isNaN(n) ? Number.MAX_SAFE_INTEGER : n;
    }
    // Extract image URLs from the embedded Next.js flight payload. The Kotlin
    // getNextJson finds the script containing "images" and grabs the JSON array
    // following it. Here we scan all <script> bodies for an "images":[...] array
    // whose objects carry a "url".
    extractNextImages(html) {
      const out = [];
      const seen = /* @__PURE__ */ new Set();
      const candidates = [];
      let idx = 0;
      while ((idx = html.indexOf("images", idx)) !== -1) {
        const arrStart = html.indexOf("[", idx);
        if (arrStart === -1) {
          idx += 6;
          continue;
        }
        if (arrStart - idx > 8) {
          idx += 6;
          continue;
        }
        let depth = 1, i = arrStart + 1;
        while (i < html.length && depth > 0) {
          const c = html[i];
          if (c === "[")
            depth++;
          else if (c === "]")
            depth--;
          i++;
        }
        candidates.push(html.substring(arrStart, i));
        idx = i;
      }
      for (let raw of candidates) {
        const cleaned = raw.replace(/\\\//g, "/").replace(/\\"/g, '"');
        let arr;
        try {
          arr = JSON.parse(cleaned);
        } catch {
          continue;
        }
        if (!Array.isArray(arr))
          continue;
        const withOrder = [];
        for (const item of arr) {
          if (!item || typeof item !== "object")
            continue;
          const url = item.url || item.src || item.image;
          if (!url || typeof url !== "string")
            continue;
          if (!/^https?:\/\//.test(url))
            continue;
          withOrder.push({ order: this.parseOrder(item.order), url: url.replace("/public//", "/public/") });
        }
        if (withOrder.length) {
          withOrder.sort((a, b) => a.order - b.order);
          for (const w of withOrder) {
            if (seen.has(w.url))
              continue;
            seen.add(w.url);
            out.push(w.url);
          }
          if (out.length)
            break;
        }
      }
      return out;
    }
  };

  // src/mmrcms.js
  var MmrcmsParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 20) {
      super(context, source, domain, pageSize);
      this.listUrl = "filterList";
      this.tagUrl = "manga-list";
      this.imgUpdated = "/cover/cover_250x350.jpg";
      this.selectDesc = "div.well";
      this.labelState = ["Statut"];
      this.labelAlt = ["Autres noms"];
      this.labelAuthor = ["Auteur(s)"];
      this.labelTag = ["Cat\xE9gories"];
      this.selectChapter = "ul.chapters > li:not(.btn)";
      this.selectDate = "div.date-chapter-title-rtl";
      this.datePattern = "dd MMM. yyyy";
      this.selectPage = "div#all img";
      this.ongoing = new Set([
        "on going",
        "ongoing",
        "en cours",
        "en curso",
        "devam ediyor",
        "\u0645\u0633\u062A\u0645\u0631\u0629"
      ].map((s) => s.toLowerCase()));
      this.finished = new Set([
        "completed",
        "completo",
        "complete",
        "termin\xE9",
        "tamamland\u0131",
        "\u0645\u0643\u062A\u0645\u0644\u0629"
      ].map((s) => s.toLowerCase()));
      this.applySourceConfig(source && source.id);
    }
    applySourceConfig(id) {
      switch (id) {
        case "ONMA":
          this.variant = "onma";
          this.labelState = ["\u0627\u0644\u062D\u0627\u0644\u0629"];
          this.labelAlt = ["\u0623\u0633\u0645\u0627\u0621 \u0623\u062E\u0631\u0649"];
          this.labelAuthor = ["\u0627\u0644\u0645\u0624\u0644\u0641"];
          this.labelTag = ["\u0627\u0644\u062A\u0635\u0646\u064A\u0641\u0627\u062A"];
          break;
        case "ANZMANGASHD":
        case "MANGADOOR":
          this.labelState = ["Estado"];
          this.labelAlt = ["Otros nombres"];
          this.labelAuthor = ["Autor(es)"];
          this.labelTag = ["Categor\xEDas"];
          break;
        case "MANGA_DENIZI":
          this.labelState = ["Durum"];
          this.labelAlt = ["Di\u011Fer Adlar\u0131"];
          this.labelAuthor = ["Yazar & \xC7izer"];
          this.labelTag = ["Kategoriler"];
          this.datePattern = "dd.MM.yyyy";
          break;
        case "READCOMICSONLINE":
          this.labelState = ["Status"];
          this.labelTag = ["Categories"];
          break;
        case "BANANASCAN_COM":
          break;
        default:
          break;
      }
      this.labelState = this.labelState.concat(["Status", "Statut", "Estado", "Durum", "Stato"]);
      this.labelAlt = this.labelAlt.concat(["Other names", "Autres noms", "Otros nombres", "Di\u011Fer Adlar\u0131", "Alt"]);
      this.labelAuthor = this.labelAuthor.concat(["Author(s)", "Auteur(s)", "Autor(es)", "Yazar", "Author"]);
      this.labelTag = this.labelTag.concat(["Categories", "Cat\xE9gories", "Categor\xEDas", "Kategoriler", "Genres", "Genre"]);
    }
    // --- helpers (mirror madara.js conventions) ---
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      if (!img)
        return "";
      let url = img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("data-original") || img.getAttribute("src") || "";
      url = url.trim();
      if (!url || url.startsWith("data:") || url.startsWith("blob:")) {
        const ds = (img.getAttribute("data-src") || "").trim();
        if (ds && !ds.startsWith("data:"))
          url = ds;
        else
          return "";
      }
      return this.toAbsoluteUrl(url);
    }
    /**
     * Find the metadata value sibling for a label. Mirrors the Kotlin
     * `dt:contains(Label)?.nextElementSibling()` pattern but works without the
     * jQuery `:contains` pseudo-selector. Scans dt/h3 elements for one whose
     * text contains any of the given labels, returns the following <dd> (or
     * nextElementSibling).
     */
    findLabelSibling(doc, labels) {
      const wanted = labels.map((l) => l.toLowerCase());
      const candidates = Array.from(doc.querySelectorAll("dt, h3, .info-label, b, strong"));
      for (const el of candidates) {
        const t = (el.textContent || "").trim().toLowerCase();
        if (!t)
          continue;
        if (wanted.some((w) => t.includes(w))) {
          let sib = el.nextElementSibling;
          const nested = el.querySelector ? el.querySelector(".text") : null;
          if (nested && (nested.textContent || "").trim())
            return nested;
          return sib || null;
        }
      }
      return null;
    }
    findLabelText(doc, labels) {
      const sib = this.findLabelSibling(doc, labels);
      const t = sib ? (sib.textContent || "").trim() : "";
      return t || null;
    }
    sortByParam(order) {
      switch (order) {
        case SortOrder.POPULARITY:
          return "views&asc=false";
        case SortOrder.POPULARITY_ASC:
          return "views&asc=true";
        case SortOrder.ALPHABETICAL:
          return "name&asc=true";
        case SortOrder.ALPHABETICAL_DESC:
          return "name&asc=false";
        default:
          return "name&asc=true";
      }
    }
    // --- list ---
    async getListPage(page, order, filter = {}) {
      const query = filter.query || "";
      const tags = filter.tags || [];
      const tagKey = tags.length ? tags[0].key || tags[0] : "";
      if (this.variant === "onma" && query) {
        if (page > 1)
          return [];
        return this.onmaSearch(query);
      }
      if (order === SortOrder.UPDATED && !query && !tags.length) {
        const url2 = `https://${this.domain}/latest-release?page=${page}`;
        const html2 = await this.context.httpGet(url2, this);
        return this.parseMangaListUpdated(html2);
      }
      const effectiveOrder = order === SortOrder.UPDATED ? SortOrder.ALPHABETICAL : order;
      const url = `https://${this.domain}/${this.listUrl}?page=${page}&author=&tag=&alpha=${encodeURIComponent(query)}&cat=${encodeURIComponent(tagKey)}&sortBy=${this.sortByParam(effectiveOrder)}`;
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    async onmaSearch(query) {
      const url = `https://${this.domain}/search?query=${encodeURIComponent(query)}`;
      let json;
      try {
        json = JSON.parse(await this.context.httpGet(url, this));
      } catch {
        return [];
      }
      const suggestions = json && json.suggestions || [];
      return suggestions.map((s) => {
        const slug = s.data;
        const href = `/manga/${slug}`;
        return new Manga({
          id: href,
          url: href,
          publicUrl: this.toAbsoluteUrl(href),
          coverUrl: `https://${this.domain}/uploads/manga/${slug}/cover/cover_250x350.jpg`,
          title: s.value || "",
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        });
      });
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, [
        "div.media",
        "div.chapter-container",
        ".manga-item",
        ".col-sm-6 .media"
      ]);
      const list = [];
      for (const el of elements) {
        const a = el.querySelector("a[href]");
        if (!a)
          continue;
        const href = this.toRelativeUrl(a.getAttribute("href"));
        if (!href)
          continue;
        const img = el.querySelector("img");
        const titleEl = el.querySelector("div.media-body h5, h5.media-heading, .media-body h5, h5, h3 a, .manga-name");
        const ratingEl = el.querySelector("span");
        const rating = ratingEl ? this.parseRating(ratingEl) : 0;
        list.push(new Manga({
          id: href,
          url: href,
          publicUrl: this.toAbsoluteUrl(href),
          coverUrl: this.imageSrc(img),
          title: titleEl ? titleEl.textContent.trim() : (a.getAttribute("title") || a.textContent || "").trim(),
          rating,
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return list;
    }
    parseRating(span) {
      const m = (span.textContent || "").match(/[\d.]+/);
      if (!m)
        return 0;
      const v = parseFloat(m[0]);
      return Number.isFinite(v) ? Math.max(0, Math.min(1, v / 5)) : 0;
    }
    parseMangaListUpdated(html) {
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, ["div.manga-item", ".manga-item", "div.media"]);
      const list = [];
      for (const el of elements) {
        const a = el.querySelector("a[href]");
        if (!a)
          continue;
        const href = this.toRelativeUrl(a.getAttribute("href"));
        if (!href)
          continue;
        const slug = href.replace(/\/+$/, "").split("/").pop();
        const titleEl = el.querySelector("h3 a, h3, .manga-name, h5");
        list.push(new Manga({
          id: href,
          url: href,
          publicUrl: this.toAbsoluteUrl(href),
          coverUrl: `https://${this.domain}/uploads/manga/${slug}${this.imgUpdated}`,
          title: titleEl ? titleEl.textContent.trim() : (a.textContent || "").trim(),
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return list;
    }
    // --- details ---
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const desc = doc.querySelector(this.selectDesc)?.textContent?.trim() || manga.description || "";
      const stateText = this.findLabelText(doc, this.labelState);
      let state;
      if (stateText) {
        const lc = stateText.toLowerCase();
        if (this.ongoing.has(lc))
          state = MangaState.ONGOING;
        else if (this.finished.has(lc))
          state = MangaState.FINISHED;
      }
      const alt = this.findLabelText(doc, this.labelAlt);
      const author = this.findLabelText(doc, this.labelAuthor);
      const tagSib = this.findLabelSibling(doc, this.labelTag);
      const tags = [];
      if (tagSib && tagSib.querySelectorAll) {
        for (const a of Array.from(tagSib.querySelectorAll("a"))) {
          const key = (a.getAttribute("href") || "").replace(/\/+$/, "").split("/").pop().replace(/.*cat=/, "");
          const title = a.textContent.trim();
          if (title)
            tags.push({ key: key || title, title });
        }
      }
      const chapters = this.parseChapters(doc);
      return new Manga({
        ...manga,
        title: doc.querySelector("h2.listmanga-header, h1, .widget-title")?.textContent?.trim() || manga.title,
        description: desc,
        altTitles: alt ? [alt] : manga.altTitles || [],
        authors: author ? [author] : manga.authors || [],
        tags: tags.length ? tags : manga.tags || [],
        state: state || manga.state,
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        chapters
      });
    }
    parseChapters(doc) {
      const elements = this.queryAll(doc, [
        this.selectChapter,
        "ul.chapters > li:not(.btn)",
        "ul.chapters li",
        "li.volume-0, li.volume-1",
        ".chapters li"
      ]).filter((li) => !(li.classList && li.classList.contains("btn")));
      const ordered = elements.slice().reverse();
      const chapters = [];
      ordered.forEach((li, i) => {
        const a = li.querySelector("a[href]");
        if (!a)
          return;
        const href = this.toRelativeUrl(a.getAttribute("href"));
        if (!href || href.includes("#"))
          return;
        const titleEl = li.querySelector("h5");
        const dateText = li.querySelector(this.selectDate)?.textContent?.trim() || li.querySelector("div.action div, .date-chapter-title-rtl")?.textContent?.trim();
        chapters.push(new MangaChapter({
          id: href,
          url: href,
          title: titleEl ? titleEl.textContent.replace(/\s+/g, " ").trim() : a.textContent.trim(),
          number: i + 1,
          volume: 0,
          uploadDate: this.parseDate(dateText),
          source: this.source
        }));
      });
      return chapters;
    }
    parseDate(text) {
      if (!text)
        return 0;
      const t = text.trim();
      let m = t.match(/^(\d{1,2})\.(\d{1,2})\.(\d{4})$/);
      if (m)
        return Date.UTC(+m[3], +m[2] - 1, +m[1]) || 0;
      m = t.match(/^(\d{1,2})\s+([A-Za-zçÇ.]+)\.?\s+(\d{4})$/);
      if (m) {
        const mo = this.monthIndex(m[2]);
        if (mo >= 0)
          return Date.UTC(+m[3], mo, +m[1]) || 0;
      }
      const parsed = Date.parse(t);
      return Number.isFinite(parsed) ? parsed : 0;
    }
    monthIndex(name) {
      const n = name.replace(/\./g, "").slice(0, 3).toLowerCase();
      const en = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"];
      let i = en.indexOf(n);
      if (i >= 0)
        return i;
      const intl = {
        jan: 0,
        f\u00E9v: 1,
        fev: 1,
        mar: 2,
        avr: 3,
        mai: 4,
        jui: 5,
        juil: 6,
        ao\u00FB: 7,
        aou: 7,
        sep: 8,
        oct: 9,
        nov: 10,
        d\u00E9c: 11,
        dec: 11,
        ene: 0,
        abr: 3,
        ago: 7,
        dic: 11,
        oca: 0,
        \u015Fub: 1,
        sub: 1,
        nis: 3,
        haz: 5,
        tem: 6,
        agu: 7,
        eyl: 8,
        eki: 9,
        kas: 10,
        ara: 11
      };
      return n in intl ? intl[n] : -1;
    }
    // --- pages ---
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      const imgs = this.queryAll(doc, [
        this.selectPage,
        "div#all img",
        "#all img",
        "div.viewer-cnt img",
        "#all .img-responsive",
        ".chapter-img img"
      ]);
      return imgs.map((img) => {
        const url = this.imageSrc(img);
        return new MangaPage({
          id: url,
          url,
          source: this.source
        });
      }).filter((p) => p.url);
    }
  };

  // src/cupfox.js
  var CupFoxParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 24) {
      super(context, source, domain, pageSize);
      this.selectMangas = "ul.row li, ul.stui-vodlist li, ul.clearfix li.dm-list, div.vod-list ul.row li, ul.ewave-vodlist li";
      this.selectMangasCover = "div.img-wrapper, div.stui-vodlist__thumb, a.stui-vodlist__thumb, div.ewave-vodlist__thumb, img.dm-thumb";
      this.selectMangaTitle = "h3, h4, p.dm-bn";
      this.selectMangaDetailsAltTitle = "div.info span:contains(Autres noms), div.info span:contains(Bi\u1EC7t danh)";
      this.selectMangaDetailsTags = "div.info span a[href*=tags], p.data a[href*=tags], div.book-main-right p.info-text a[href*=tags]";
      this.selectMangaDetailsAuthor = "div.info span:contains(Auteur(s)), div.info span:contains(T\xE1c gi\u1EA3), p.data span:contains(Auteur(s)), p.data span:contains(Autor), p.data span:contains(\u4F5C\u8005), div.book-main-right div.book-info:contains(\u4F5C\u8005) .info-text";
      this.selectMangaDescription = "div.vod-list:contains(R\xE9sum\xE9) div.more-box, div.stui-pannel__head:contains(R\xE9sum\xE9), div.book-desc div.info-text, div.info div.text:contains(Gi\u1EDBi thi\u1EC7u), #desc";
      this.selectMangaChapters = "div.episode-box ul li, ul.stui-content__playlist li a, ul.cnxh-ul li a, ul.ewave-content__playlist li a";
      this.selectPages = "div.more-box li img, ul.main li img";
      this.selectAvailableTags = "div.swiper-wrapper a[href*=tags], ul.stui-screen__list li a[href*=tags]";
      this.searchPath = "/search/";
      this.categoryPath = "/category/";
      this.orderPopularity = "order/hits/";
      this.orderUpdated = "order/addtime/";
      this.stateOngoing = "finish/1/";
      this.stateFinished = "finish/2/";
    }
    /**
     * Try each selector in order, returning the first non-empty match list.
     * Mirrors madara.js so minor markup drift / unsupported `:contains()` in
     * the browser DOM doesn't kill extraction.
     */
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    queryFirst(el, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const found = el.querySelector(selector);
          if (found)
            return found;
        } catch {
        }
      }
      return null;
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-original") || img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // Kotlin requireSrc(): also honours data-original/data-src lazy attrs.
    requireSrc(img) {
      return this.imageSrc(img);
    }
    contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    /** key = last path segment of href (after stripping a trailing slash). */
    keyFromHref(href) {
      const clean = (href || "").replace(/\/+$/, "");
      const i = clean.lastIndexOf("/");
      return i >= 0 ? clean.slice(i + 1) : clean;
    }
    titleCase(text) {
      return (text || "").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
    }
    /** text after the last full-width or ASCII colon (mirrors substringAfter("：")). */
    afterColon(text) {
      if (!text)
        return "";
      const t = text.trim();
      const idx = t.lastIndexOf("\uFF1A");
      if (idx >= 0)
        return t.slice(idx + 1).trim();
      const idx2 = t.lastIndexOf(":");
      return idx2 >= 0 ? t.slice(idx2 + 1).trim() : t;
    }
    async getListPage(page, order, filter) {
      filter = filter || {};
      const pageNo = page || 1;
      let url = `https://${this.domain}`;
      if (filter.query) {
        url += this.searchPath + encodeURIComponent(filter.query) + "/" + pageNo;
      } else {
        url += this.categoryPath;
        url += order === SortOrder.POPULARITY ? this.orderPopularity : this.orderUpdated;
        const state = this.oneState(filter.states);
        if (state === MangaState.ONGOING)
          url += this.stateOngoing;
        else if (state === MangaState.FINISHED)
          url += this.stateFinished;
        const tag = this.oneTag(filter.tags);
        if (tag)
          url += "tags/" + tag + "/";
        url += "page/" + pageNo;
      }
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    oneState(states) {
      if (!states)
        return null;
      if (Array.isArray(states))
        return states.length ? states[0] : null;
      return states;
    }
    oneTag(tags) {
      if (!tags)
        return null;
      let t = tags;
      if (Array.isArray(tags))
        t = tags.length ? tags[0] : null;
      if (!t)
        return null;
      return typeof t === "string" ? t : t.key || null;
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const items = this.queryAll(doc, [
        this.selectMangas,
        "ul.stui-vodlist li",
        "ul.ewave-vodlist li",
        "div.vod-list ul.row li",
        "ul.row li",
        "ul.clearfix li.dm-list"
      ]);
      const list = [];
      for (const li of items) {
        const a = li.querySelector("a");
        if (!a)
          continue;
        const href = this.toRelativeUrl(a.getAttribute("href"));
        if (!href)
          continue;
        const titleEl = this.queryFirst(li, [this.selectMangaTitle, "h3", "h4", "p.dm-bn"]);
        const coverEl = this.queryFirst(li, [
          this.selectMangasCover,
          "div.stui-vodlist__thumb",
          "a.stui-vodlist__thumb",
          "div.ewave-vodlist__thumb",
          "div.img-wrapper",
          "img.dm-thumb"
        ]);
        const coverImg = coverEl && coverEl.tagName === "IMG" ? coverEl : (coverEl ? coverEl.querySelector("img") : null) || li.querySelector("img");
        list.push(new Manga({
          id: href,
          url: href,
          publicUrl: this.toAbsoluteUrl(href),
          coverUrl: this.imageSrc(coverImg),
          title: titleEl ? titleEl.textContent.trim() : (a.getAttribute("title") || a.textContent || "").trim(),
          source: this.source,
          contentRating: this.contentRating()
        }));
      }
      return list;
    }
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const altEl = this.queryFirst(doc, [this.selectMangaDetailsAltTitle]);
      const altTitle = altEl ? this.afterColon(altEl.textContent) : "";
      const authorEl = this.queryFirst(doc, [this.selectMangaDetailsAuthor]);
      const author = authorEl ? this.afterColon(authorEl.textContent) : "";
      const descEl = this.queryFirst(doc, [
        this.selectMangaDescription,
        "div.book-desc div.info-text",
        "#desc"
      ]);
      const description = descEl ? descEl.innerHTML : "";
      const tagEls = this.queryAll(doc, [
        this.selectMangaDetailsTags,
        "a[href*=tags]"
      ]);
      const tags = tagEls.map((a) => ({
        key: this.keyFromHref(a.getAttribute("href")),
        title: this.titleCase(a.textContent.trim()),
        source: this.source
      }));
      const chapterEls = this.queryAll(doc, [
        this.selectMangaChapters,
        "div.episode-box ul li",
        "ul.stui-content__playlist li a",
        "ul.ewave-content__playlist li a",
        "ul.cnxh-ul li a"
      ]);
      const chapters = chapterEls.map((el, i) => {
        const a = el.tagName === "A" ? el : el.querySelector("a");
        if (!a)
          return null;
        const href = this.toRelativeUrl(a.getAttribute("href"));
        if (!href || href.includes("#"))
          return null;
        return new MangaChapter({
          id: href,
          url: href,
          title: a.textContent.trim(),
          number: i + 1,
          volume: 0,
          source: this.source
        });
      }).filter(Boolean);
      return new Manga({
        ...manga,
        altTitles: altTitle ? [altTitle] : [],
        authors: author ? [author] : [],
        tags,
        description,
        chapters
      });
    }
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      const imgs = this.queryAll(doc, [
        this.selectPages,
        "div.more-box li img",
        "ul.main li img"
      ]);
      return imgs.map((img) => {
        const url = this.imageSrc(img);
        return new MangaPage({
          id: url,
          url,
          source: this.source
        });
      }).filter((p) => p.url);
    }
  };

  // src/fmreader.js
  var FmreaderParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 20) {
      super(context, source, domain, pageSize);
      this.listUrl = "/manga-list.html";
      this.datePattern = "MMMM d, yyyy";
      this.tagPrefix = "manga-list-genre-";
      this.selectMangaList = "div.thumb-item-flow";
      this.selectMangaTitleLink = "div.series-title a";
      this.selectMangaTitle = "div.series-title";
      this.selectMangaCover = "div.img-in-ratio";
      this.selectDesc = "div.summary-content";
      this.selectState = "ul.manga-info li:contains(Status) a";
      this.selectAlt = "ul.manga-info li:contains(Other names)";
      this.selectAut = "ul.manga-info li:contains(Author(s)) a";
      this.selectTag = "ul.manga-info li:contains(Genre(s)) a";
      this.selectChapter = "ul.list-chapters a";
      this.selectChapterName = "div.chapter-name";
      this.selectDate = "div.chapter-time";
      this.selectPage = "div.chapter-content img";
      this.ongoing = /* @__PURE__ */ new Set(["on going", "ongoing", "incomplete", "en curso"]);
      this.finished = /* @__PURE__ */ new Set(["completed", "completado", "complete"]);
      this.abandoned = /* @__PURE__ */ new Set(["canceled", "cancelled", "drop", "dropped"]);
      this.useApi = domain === "klz9.com";
      this.apiClientSecret = "KL9K40zaSyC9K40vOMLLbEcepIFBhUKXwELqxlwTEF";
      this.apiListLimit = 36;
    }
    /** Try selectors in order; return first non-empty match list (madara-style). */
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    /**
     * Resolve a reader/cover image src. Fmreader lazy-loads page images with the
     * real URL base64-encoded in `data-img`; covers/list thumbs use plain attrs.
     */
    imageSrc(img) {
      if (!img)
        return "";
      const encoded = img.getAttribute("data-img");
      if (encoded && /^[A-Za-z0-9+/=\s]+$/.test(encoded)) {
        const decoded = this.decodeBase64(encoded.trim());
        if (decoded && decoded.startsWith("http"))
          return decoded;
      }
      const url = img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("data-original") || img.getAttribute("src") || "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return "";
      if (url.endsWith("loading.gif"))
        return "";
      return this.toAbsoluteUrl(url);
    }
    decodeBase64(str) {
      try {
        if (typeof atob === "function")
          return atob(str);
        if (typeof Buffer !== "undefined")
          return Buffer.from(str, "base64").toString("utf8");
      } catch {
      }
      return "";
    }
    /**
     * jQuery-style ":contains(text)" emulation: find an <li> in `doc` whose text
     * includes `label`. Returns the matching element or null.
     */
    findInfoItem(doc, label) {
      const items = doc.querySelectorAll("ul.manga-info li");
      const want = label.toLowerCase();
      for (const li of items) {
        if ((li.textContent || "").toLowerCase().includes(want))
          return li;
      }
      return null;
    }
    parseState(text) {
      const t = (text || "").trim().toLowerCase();
      if (!t)
        return void 0;
      if (this.ongoing.has(t))
        return MangaState.ONGOING;
      if (this.finished.has(t))
        return MangaState.FINISHED;
      if (this.abandoned.has(t))
        return MangaState.ABANDONED;
      for (const v of this.ongoing)
        if (t.includes(v))
          return MangaState.ONGOING;
      for (const v of this.finished)
        if (t.includes(v))
          return MangaState.FINISHED;
      for (const v of this.abandoned)
        if (t.includes(v))
          return MangaState.ABANDONED;
      return void 0;
    }
    // ============================ LIST ============================
    async getListPage(page, order, filter) {
      if (this.useApi)
        return this.getApiListPage(page, order, filter);
      let url = `https://${this.domain}${this.listUrl}?page=${page}`;
      if (filter && filter.query) {
        url += `&name=${encodeURIComponent(filter.query)}`;
      }
      const genres = (filter && filter.tags || []).map((t) => t.key).join(",");
      url += `&genre=${genres}`;
      url += `&ungenre=`;
      url += `&sort=`;
      switch (order) {
        case SortOrder.POPULARITY:
          url += "views&sort_type=DESC";
          break;
        case SortOrder.POPULARITY_ASC:
          url += "views&sort_type=ASC";
          break;
        case SortOrder.UPDATED:
          url += "last_update&sort_type=DESC";
          break;
        case SortOrder.UPDATED_ASC:
          url += "last_update&sort_type=ASC";
          break;
        case SortOrder.ALPHABETICAL:
          url += "name&sort_type=ASC";
          break;
        case SortOrder.ALPHABETICAL_DESC:
          url += "name&sort_type=DESC";
          break;
        default:
          url += "last_update&sort_type=DESC";
          break;
      }
      url += `&m_status=`;
      const state = filter && filter.states && filter.states.length === 1 ? filter.states[0] : null;
      if (state === MangaState.ONGOING)
        url += "2";
      else if (state === MangaState.FINISHED)
        url += "1";
      else if (state === MangaState.ABANDONED)
        url += "3";
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const items = this.queryAll(doc, [this.selectMangaList, "div.thumb-item-flow"]);
      const mangaList = [];
      for (const div of items) {
        const a = div.querySelector(this.selectMangaTitleLink) || div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const titleEl = div.querySelector(this.selectMangaTitle);
        mangaList.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.extractCover(div),
          title: titleEl ? titleEl.textContent.trim() : (a.textContent || "").trim(),
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return mangaList;
    }
    extractCover(div) {
      const imgDiv = div.querySelector(this.selectMangaCover);
      if (!imgDiv) {
        return this.imageSrc(div.querySelector("img"));
      }
      const dataBg = imgDiv.getAttribute("data-bg");
      if (dataBg)
        return this.toAbsoluteUrl(dataBg);
      const style = imgDiv.getAttribute("style") || "";
      const m = style.match(/url\(\s*['"]?([^'")]+)['"]?\s*\)/i);
      if (m)
        return this.toAbsoluteUrl(m[1]);
      return this.imageSrc(imgDiv.querySelector("img"));
    }
    // ============================ DETAILS ============================
    async getDetails(manga) {
      if (this.useApi)
        return this.getApiDetails(manga);
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const descEl = doc.querySelector(this.selectDesc);
      const description = descEl ? descEl.innerHTML : "";
      const stateLi = this.findInfoItem(doc, "Status");
      const stateLink = stateLi ? stateLi.querySelector("a") : null;
      const state = this.parseState(stateLink ? stateLink.textContent : stateLi ? stateLi.textContent : "");
      const altLi = this.findInfoItem(doc, "Other name");
      const altTitle = altLi ? altLi.textContent.replace(/Other names?\s*\(?s?\)?:?/i, "").trim() : "";
      const autLi = this.findInfoItem(doc, "Author");
      const authorLink = autLi ? autLi.querySelector("a") : null;
      const author = authorLink ? authorLink.textContent.trim() : autLi ? autLi.textContent.replace(/Author\(s\):?/i, "").trim() : "";
      const tagLi = this.findInfoItem(doc, "Genre");
      const tags = [];
      if (tagLi) {
        for (const a of tagLi.querySelectorAll("a")) {
          const href = a.getAttribute("href") || "";
          const key = href.split(this.tagPrefix).pop().replace(/\.html$/, "");
          tags.push({ key, title: a.textContent.trim(), source: this.source });
        }
      }
      const chapters = await this.getChapters(manga, doc);
      return new Manga({
        ...manga,
        description,
        altTitles: altTitle ? [altTitle] : manga.altTitles || [],
        authors: author ? [author] : manga.authors || [],
        tags: tags.length ? tags : manga.tags || [],
        state: state || manga.state,
        chapters
      });
    }
    // ============================ CHAPTERS ============================
    /**
     * @param {Manga} manga  the manga being detailed (for sources that need its url)
     * @param {Document} doc the already-parsed details document
     */
    async getChapters(manga, doc) {
      const anchors = this.queryAll(doc, [this.selectChapter, "ul.list-chapters a", "div.list-chapters a"]);
      return this.buildChapters(anchors);
    }
    /** Build oldest-first chapter list from a set of <a> elements (newest-first in DOM). */
    buildChapters(anchors) {
      const reversed = anchors.slice().reverse();
      const chapters = [];
      reversed.forEach((a, i) => {
        const href = a.getAttribute("href");
        if (!href || href.includes("#"))
          return;
        const relHref = this.toRelativeUrl(href);
        const nameEl = a.querySelector(this.selectChapterName);
        const dateEl = a.querySelector(this.selectDate);
        const title = nameEl ? nameEl.textContent.trim() : a.textContent.trim();
        chapters.push(new MangaChapter({
          id: relHref,
          url: relHref,
          title,
          number: i + 1,
          volume: 0,
          uploadDate: this.parseChapterDate(dateEl ? dateEl.textContent.trim() : ""),
          source: this.source
        }));
      });
      return chapters;
    }
    // ============================ PAGES ============================
    async getPages(chapter) {
      if (this.useApi)
        return this.getApiPages(chapter);
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      return this.extractPages(doc);
    }
    extractPages(doc) {
      const imgs = this.queryAll(doc, [this.selectPage, "div.chapter-content img", "div.reading-content img"]);
      const pages = [];
      for (const img of imgs) {
        const url = this.imageSrc(img);
        if (!url)
          continue;
        pages.push(new MangaPage({ id: url, url, source: this.source }));
      }
      return pages;
    }
    // ================== KLZ9 JSON API IMPLEMENTATION ==================
    async createApiHeaders() {
      const timestamp = Math.floor(Date.now() / 1e3).toString();
      const signature = await this.sha256Hex(`${timestamp}.${this.apiClientSecret}`);
      return {
        "Content-Type": "application/json",
        "x-client-ts": timestamp,
        "x-client-sig": signature
      };
    }
    async sha256Hex(message) {
      const cryptoObj = typeof crypto !== "undefined" && crypto.subtle ? crypto : typeof globalThis !== "undefined" ? globalThis.crypto : null;
      if (cryptoObj && cryptoObj.subtle) {
        const enc = new TextEncoder().encode(message);
        const buf = await cryptoObj.subtle.digest("SHA-256", enc);
        return Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, "0")).join("");
      }
      const nodeCrypto = await Promise.resolve().then(() => (init_node_crypto_shim(), node_crypto_shim_exports));
      return nodeCrypto.createHash("sha256").update(message).digest("hex");
    }
    async apiGetJson(url) {
      const headers = await this.createApiHeaders();
      const text = await this.context.httpGet(url, this, headers);
      return JSON.parse(text);
    }
    async getApiListPage(page, order, filter) {
      const params = new URLSearchParams();
      params.set("page", String(page));
      params.set("limit", String(this.apiListLimit));
      if (filter && filter.query)
        params.set("search", filter.query);
      switch (order) {
        case SortOrder.POPULARITY:
          params.set("sort", "Popular");
          params.set("order", "desc");
          break;
        case SortOrder.UPDATED:
          params.set("sort", "last_update");
          params.set("order", "desc");
          break;
        case SortOrder.ALPHABETICAL:
          params.set("sort", "name");
          params.set("order", "asc");
          break;
        case SortOrder.ALPHABETICAL_DESC:
          params.set("sort", "name");
          params.set("order", "desc");
          break;
        default:
          params.set("sort", "Popular");
          params.set("order", "desc");
          break;
      }
      const url = `https://${this.domain}/api/manga/list?${params.toString()}`;
      const json = await this.apiGetJson(url);
      const items = Array.isArray(json.items) ? json.items : [];
      return items.map((jo) => new Manga({
        id: jo.slug,
        url: jo.slug,
        publicUrl: `https://${this.domain}/${jo.slug}`,
        coverUrl: jo.cover || "",
        title: jo.name || jo.slug,
        source: this.source,
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
      }));
    }
    async getApiDetails(manga) {
      const slug = manga.url;
      const json = await this.apiGetJson(`https://${this.domain}/api/manga/slug/${slug}`);
      const tags = String(json.genres || "").split(",").map((g) => g.trim()).filter(Boolean).map((g) => ({ key: g.toLowerCase().replace(/\s+/g, "-"), title: g, source: this.source }));
      let state;
      switch (json.m_status) {
        case 1:
          state = MangaState.FINISHED;
          break;
        case 2:
          state = MangaState.ONGOING;
          break;
        case 3:
          state = MangaState.PAUSED;
          break;
      }
      const authors = [json.authors, json.artists].map((x) => (x || "").trim()).filter(Boolean);
      const alt = (json.other_name || "").trim();
      return new Manga({
        ...manga,
        title: json.name || manga.title,
        description: json.description || "",
        coverUrl: json.cover || manga.coverUrl,
        altTitles: alt ? [alt] : manga.altTitles || [],
        authors: authors.length ? authors : manga.authors || [],
        tags: tags.length ? tags : manga.tags || [],
        state: state || manga.state,
        chapters: this.parseApiChapters(json)
      });
    }
    parseApiChapters(data) {
      const arr = Array.isArray(data.chapters) ? data.chapters : [];
      const chapters = arr.map((obj) => {
        const number = this.apiChapterNumber(obj);
        const ctitle = this.apiChapterTitle(obj);
        const formatted = Number.isInteger(number) ? String(number) : String(number);
        const title = ctitle ? `Chapter ${formatted}: ${ctitle}` : `Chapter ${formatted}`;
        return new MangaChapter({
          id: String(obj.id),
          url: String(obj.id),
          title,
          number,
          volume: 0,
          uploadDate: this.parseIsoDate(obj.last_update || ""),
          source: this.source
        });
      });
      chapters.sort((a, b) => a.number - b.number || a.uploadDate - b.uploadDate || Number(a.id) - Number(b.id));
      return chapters;
    }
    apiChapterNumber(obj) {
      const direct = obj.chapter !== void 0 && obj.chapter !== null && obj.chapter !== "" ? Number(obj.chapter) : obj.number !== void 0 && obj.number !== null && obj.number !== "" ? Number(obj.number) : NaN;
      if (!Number.isNaN(direct))
        return direct;
      const raw = [obj.chapter, obj.number, obj.name, obj.title].map((x) => x == null ? "" : String(x)).find((x) => x && x.toLowerCase() !== "null") || "";
      const m = raw.match(/(\d+(?:\.\d+)?)/);
      return m ? parseFloat(m[1]) : 0;
    }
    apiChapterTitle(obj) {
      const candidates = [obj.name, obj.title, obj.chapter_name, obj.chapter_title];
      for (const c of candidates) {
        const t = (c == null ? "" : String(c)).trim();
        if (t && t.toLowerCase() !== "null")
          return t;
      }
      return null;
    }
    async getApiPages(chapter) {
      const json = await this.apiGetJson(`https://${this.domain}/api/chapter/${chapter.url}`);
      const content = json.content || "";
      if (content) {
        const urls = content.split(/\r\n|\r|\n/).map((s) => s.trim()).filter((s) => s && s.startsWith("http"));
        if (urls.length) {
          return urls.map((url) => new MangaPage({ id: url, url, source: this.source }));
        }
      }
      return [];
    }
    // ============================ DATES ============================
    parseIsoDate(s) {
      if (!s)
        return 0;
      const t = Date.parse(s);
      return Number.isNaN(t) ? 0 : t;
    }
    /** Parse the HTML-template chapter dates: relative ("4 hours ago"), "today", or absolute. */
    parseChapterDate(date) {
      if (!date)
        return 0;
      const d = date.toLowerCase();
      if (/\bago\b|\batrás\b/.test(d) || /\d+\s*[hd]\b/.test(d)) {
        return this.parseRelativeDate(d);
      }
      if (d.startsWith("today")) {
        const c = /* @__PURE__ */ new Date();
        c.setHours(0, 0, 0, 0);
        return c.getTime();
      }
      const t = Date.parse(date);
      return Number.isNaN(t) ? 0 : t;
    }
    parseRelativeDate(date) {
      const m = date.match(/(\d+)/);
      if (!m)
        return 0;
      const n = parseInt(m[1], 10);
      const now = /* @__PURE__ */ new Date();
      if (/\bsecond/.test(date))
        now.setSeconds(now.getSeconds() - n);
      else if (/\bmin|minute|minuto/.test(date))
        now.setMinutes(now.getMinutes() - n);
      else if (/\bhour|hora|\bh\b/.test(date))
        now.setHours(now.getHours() - n);
      else if (/\bday|día|\bdia|\bd\b/.test(date))
        now.setDate(now.getDate() - n);
      else if (/\bweek|semana/.test(date))
        now.setDate(now.getDate() - n * 7);
      else if (/\bmonth|\bmes|meses/.test(date))
        now.setMonth(now.getMonth() - n);
      else if (/\byear|año/.test(date))
        now.setFullYear(now.getFullYear() - n);
      else
        return 0;
      return now.getTime();
    }
  };
  var WeLoveMangaParser = class extends FmreaderParser {
    async getChapters(manga, doc) {
      const input = doc.querySelector("div.cmt input");
      const mid = input ? input.getAttribute("value") : null;
      if (!mid) {
        return super.getChapters(manga, doc);
      }
      const html = await this.context.httpGet(
        `https://${this.domain}/app/manga/controllers/cont.Listchapter.php?mid=${mid}`,
        this
      );
      const listDoc = this.context.parseHTML(html);
      const anchors = this.queryAll(listDoc, [this.selectChapter, "ul.list-chapters a", "a"]);
      return this.buildChapters(anchors);
    }
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      const input = doc.querySelector("#chapter");
      const cid = input ? input.getAttribute("value") : null;
      if (!cid)
        return this.extractPages(doc);
      const imgHtml = await this.context.httpGet(
        `https://${this.domain}/app/manga/controllers/cont.listImg.php?cid=${cid}`,
        this
      );
      const imgDoc = this.context.parseHTML(imgHtml);
      const imgs = Array.from(imgDoc.querySelectorAll("img"));
      const pages = [];
      for (const img of imgs) {
        const url = this.imageSrc(img);
        if (!url)
          continue;
        pages.push(new MangaPage({ id: url, url, source: this.source }));
      }
      return pages;
    }
  };

  // src/animebootstrap.js
  var AnimeBootstrapParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 24) {
      super(context, source, domain, pageSize);
      this.listUrl = "/manga";
      this.datePattern = "dd MMM. yyyy";
      this.selectMangaList = "div.col-6 div.product__item";
      this.selectMangaListLink = "a";
      this.selectMangaListPic = "div.product__item__pic";
      this.selectMangaListTitle = "div.product__item__text";
      this.coverAttr = "data-setbg";
      this.selectDesc = "div.anime__details__text p";
      this.selectState = "div.anime__details__widget li:contains(Ongoing)";
      this.selectTag = "div.anime__details__widget li:contains(Categorie) a";
      this.selectChapter = "div.anime__details__episodes a";
      this.selectPage = "div.read-img img";
    }
    // Try a list of selectors, returning the first non-empty match. Mirrors the
    // madara.js/mangareader.js fallback helper so minor markup drift is tolerated.
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    querySelectorSafe(root, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const el = root.querySelector(selector);
          if (el)
            return el;
        } catch {
        }
      }
      return null;
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // Covers are background images on `div.product__item__pic[data-setbg]`.
    coverFrom(card) {
      const pic = this.querySelectorSafe(card, [this.selectMangaListPic, "div.product__item__pic", ".product__item__pic"]);
      if (pic) {
        const bg = pic.getAttribute(this.coverAttr) || pic.getAttribute("data-setbg") || pic.getAttribute("data-bg");
        if (bg)
          return this.toAbsoluteUrl(bg);
        const inner = pic.querySelector("img");
        if (inner)
          return this.imageSrc(inner);
      }
      const img = card.querySelector("img");
      return img ? this.imageSrc(img) : "";
    }
    contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    async getListPage(page, order, filter = {}) {
      let url = `https://${this.domain}${this.listUrl}?page=${page}&type=all`;
      if (filter.query) {
        url += `&search=${encodeURIComponent(filter.query)}`;
      }
      const tag = filter.tags && filter.tags.length ? filter.tags[0] : null;
      if (tag && tag.key) {
        url += `&categorie=${encodeURIComponent(tag.key)}`;
      }
      url += "&sort=";
      switch (order) {
        case SortOrder.POPULARITY:
          url += "view";
          break;
        case SortOrder.UPDATED:
          url += "updated";
          break;
        case SortOrder.ALPHABETICAL:
          url += "default";
          break;
        case SortOrder.NEWEST:
          url += "published";
          break;
        default:
          url += "updated";
          break;
      }
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const cards = this.queryAll(doc, [
        this.selectMangaList,
        "div.col-6 div.product__item",
        "div.product__item",
        ".product__item"
      ]);
      const mangaList = [];
      const seen = /* @__PURE__ */ new Set();
      for (const card of cards) {
        const a = this.querySelectorSafe(card, [this.selectMangaListLink, "h5 a", "a"]);
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        if (seen.has(relHref))
          continue;
        const titleEl = this.querySelectorSafe(card, [
          this.selectMangaListTitle,
          "div.product__item__text h5",
          "div.product__item__text",
          ".product__item__text",
          "h5"
        ]);
        const title = (titleEl ? titleEl.textContent : a.getAttribute("title") || a.textContent || "").trim();
        if (!title)
          continue;
        seen.add(relHref);
        mangaList.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.coverFrom(card),
          title,
          source: this.source,
          contentRating: this.contentRating()
        }));
      }
      return mangaList;
    }
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const title = (this.querySelectorSafe(doc, ["div.anime__details__title h3", "div.anime__details__text h3", "h1", "h3"])?.textContent || manga.title || "").trim();
      const descEl = this.querySelectorSafe(doc, [
        this.selectDesc,
        "div.anime__details__text p",
        "div.anime__details__text",
        "div.entry-content"
      ]);
      const description = descEl ? descEl.innerHTML : manga.description || "";
      const stateEls = this.queryAll(doc, [
        this.selectState,
        "div.anime__details__widget li:contains(Ongoing)"
      ]);
      let state;
      if (stateEls.length) {
        state = MangaState.ONGOING;
      } else {
        const widget = this.querySelectorSafe(doc, ["div.anime__details__widget", ".anime__details__widget"]);
        const widgetText = widget ? widget.textContent.toLowerCase() : "";
        const ongoingHit = /ongoing|en cours|berjalan|berlangsung|publishing|连载/.test(widgetText);
        const finishedHit = /completed|complete|tamat|finished|terminé|selesai|完结/.test(widgetText);
        state = ongoingHit ? MangaState.ONGOING : finishedHit ? MangaState.FINISHED : MangaState.FINISHED;
      }
      const tagAnchors = this.queryAll(doc, [
        this.selectTag,
        "div.anime__details__widget li:contains(Categorie) a",
        "div.anime__details__widget li:contains(Genre) a",
        "div.anime__details__widget a[href*='categorie']",
        "div.anime__details__widget a[href*='genre']"
      ]);
      const tags = tagAnchors.map((a) => {
        const href = a.getAttribute("href") || "";
        let key;
        if (href.includes("=")) {
          key = href.split("=").pop();
        } else {
          key = href.replace(/\/$/, "").split("/").pop();
        }
        return {
          key: (key || "").trim(),
          title: a.textContent.trim().replace(/,/g, "")
        };
      }).filter((t) => t.key || t.title);
      const chapters = this.getChapters(doc);
      return new Manga({
        ...manga,
        title: title || manga.title,
        description,
        state,
        tags: tags.length ? tags : manga.tags,
        contentRating: this.contentRating(),
        source: this.source,
        chapters
      });
    }
    // Returns chapters OLDEST-FIRST (Kotlin mapChapters(reversed = true)).
    getChapters(doc) {
      const anchors = this.queryAll(doc, [
        this.selectChapter,
        "div.anime__details__episodes a",
        "ul.chapters li a",
        "ul.chapters li",
        ".chapter-list li a"
      ]);
      const reversed = anchors.slice().reverse();
      const chapters = [];
      const seen = /* @__PURE__ */ new Set();
      reversed.forEach((node, i) => {
        const a = node.tagName && node.tagName.toLowerCase() === "a" ? node : node.querySelector("a");
        if (!a)
          return;
        const href = a.getAttribute("href");
        if (!href)
          return;
        const relHref = this.toRelativeUrl(href);
        if (!relHref || relHref.includes("#") || seen.has(relHref))
          return;
        seen.add(relHref);
        const titleEl = this.querySelectorSafe(node, ["span em", "span.chapter-title", ".chapternum"]);
        const title = (titleEl ? titleEl.textContent : a.textContent || "").trim() || `Chapter ${i + 1}`;
        const dateEl = this.querySelectorSafe(node, ["span.date-chapter-title-rtl", ".date-chapter-title-rtl", ".date"]);
        chapters.push(new MangaChapter({
          id: relHref,
          url: relHref,
          title,
          number: i + 1,
          volume: 0,
          uploadDate: 0,
          branch: null,
          scanlator: null,
          source: this.source
        }));
      });
      return chapters;
    }
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      if (html.includes("page_image")) {
        const pages2 = this.parsePagesFromScript(html);
        if (pages2.length)
          return pages2;
      }
      const doc = this.context.parseHTML(html);
      const imgs = this.queryAll(doc, [
        this.selectPage,
        "div.read-img img",
        ".read-img img",
        "#readerarea img",
        "div.reading-content img"
      ]);
      const pages = [];
      imgs.forEach((img) => {
        let url = "";
        const onerror = img.getAttribute("onerror") || "";
        if (onerror.includes("this.src")) {
          url = onerror.replace("this.onerror=null;this.src=`", "").replace(/`;?\s*$/, "").replace(/^this\.onerror=null;this\.src=["'`]/, "").replace(/["'`];?\s*$/, "").trim();
        }
        if (!url)
          url = this.imageSrc(img);
        if (!url)
          return;
        const abs = this.toAbsoluteUrl(url);
        pages.push(new MangaPage({
          id: abs,
          url: abs,
          source: this.source
        }));
      });
      return pages;
    }
    // Pull the `var pages = [...]` array out of inline scripts and parse it as
    // JSON, then map each {page_image} entry to a MangaPage.
    parsePagesFromScript(html) {
      const candidates = [];
      let m = html.match(/var\s+pages\s*=\s*(\[[\s\S]*?\])\s*;/);
      if (m)
        candidates.push(m[1]);
      if (!candidates.length) {
        const arrays = html.match(/\[[\s\S]*?page_image[\s\S]*?\]/g) || [];
        candidates.push(...arrays);
      }
      for (const raw of candidates) {
        try {
          const arr = JSON.parse(raw);
          if (Array.isArray(arr) && arr.length) {
            const pages = arr.map((entry) => {
              const url = entry && (entry.page_image || entry.image || entry.url);
              if (!url)
                return null;
              const abs = this.toAbsoluteUrl(url);
              return new MangaPage({ id: abs, url: abs, source: this.source });
            }).filter(Boolean);
            if (pages.length)
              return pages;
          }
        } catch {
        }
      }
      const urls = [];
      for (const mm of html.matchAll(/["']page_image["']\s*:\s*["']([^"']+)["']/g)) {
        urls.push(mm[1]);
      }
      return urls.map((u) => {
        const abs = this.toAbsoluteUrl(u.replace(/\\\//g, "/"));
        return new MangaPage({ id: abs, url: abs, source: this.source });
      });
    }
  };

  // src/guya.js
  var GuyaParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 1e3) {
      super(context, source, domain, pageSize);
      this.allSeriesPath = "/api/get_all_series/";
      this.seriesApiPath = "/api/series/";
      this.readPath = "/read/manga/";
      this.mediaPath = "/media/manga/";
    }
    contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    async getJson(url) {
      const text = await this.context.httpGet(url, this);
      return JSON.parse(text);
    }
    // Series API URLs need a trailing slash; the backend 301-redirects
    // /api/series/<slug> -> /api/series/<slug>/ and the redirect can drop the
    // request through some proxies. Normalize defensively.
    withTrailingSlash(url) {
      return url.endsWith("/") ? url : url + "/";
    }
    seriesApiUrl(slug) {
      return this.withTrailingSlash(`https://${this.domain}${this.seriesApiPath}${slug}`);
    }
    async getListPage(page, order, filter) {
      if (page && page > 1)
        return [];
      const url = `https://${this.domain}${this.allSeriesPath}`;
      const json = await this.getJson(url);
      const query = filter && filter.query ? String(filter.query).toLowerCase() : "";
      const list = [];
      for (const name of Object.keys(json)) {
        const entry = json[name];
        if (!entry || typeof entry !== "object" || Array.isArray(entry))
          continue;
        if (!entry.slug)
          continue;
        if (query && !name.toLowerCase().includes(query))
          continue;
        list.push(this.toManga(entry, name));
      }
      return list;
    }
    toManga(entry, name) {
      const slug = entry.slug;
      const apiUrl = this.seriesApiUrl(slug);
      const publicUrl = `https://${this.domain}${this.readPath}${slug}`;
      const cover = entry.cover ? this.toAbsoluteUrl(entry.cover) : "";
      const author = entry.author || entry.artist;
      return new Manga({
        id: apiUrl,
        url: apiUrl,
        publicUrl,
        coverUrl: cover,
        largeCoverUrl: cover,
        title: name,
        altTitles: [],
        rating: 0,
        tags: [],
        description: entry.description || "",
        state: null,
        authors: author ? [author] : [],
        contentRating: this.contentRating(),
        source: this.source
      });
    }
    async getDetails(manga) {
      const apiUrl = this.withTrailingSlash(this.toAbsoluteUrl(manga.url));
      const data = await this.getJson(apiUrl);
      const json = data.chapters || {};
      const slug = data.slug || this.lastSegment(this.stripTrailingSlash(manga.url));
      const chapters = [];
      const keys = Object.keys(json);
      keys.forEach((key, i) => {
        const chapter = json[key] || {};
        const url = `https://${this.domain}${this.seriesApiPath}${slug}/${key}`;
        chapters.push(new MangaChapter({
          id: url,
          url,
          title: chapter.title || `Chapter ${key}`,
          number: i + 1,
          volume: Number(chapter.volume) || 0,
          branch: null,
          scanlator: null,
          uploadDate: 0,
          source: this.source
        }));
      });
      return new Manga({ ...manga, chapters });
    }
    async getPages(chapter) {
      const stripped = this.stripTrailingSlash(chapter.url);
      const key = this.lastSegment(stripped);
      const seriesUrl = stripped.slice(0, stripped.length - key.length - 1);
      const slug = this.lastSegment(seriesUrl);
      const data = await this.getJson(this.withTrailingSlash(seriesUrl));
      const chapters = data.chapters || {};
      const chapterData = chapters[key];
      if (!chapterData)
        return [];
      const folder = chapterData.folder;
      const images = chapterData.groups || {};
      const groupKeys = Object.keys(images);
      if (!groupKeys.length)
        return [];
      const firstKey = groupKeys[0];
      const files = images[firstKey] || [];
      return files.map((file) => {
        const url = `https://${this.domain}${this.mediaPath}${slug}/chapters/${folder}/${firstKey}/${file}`;
        return new MangaPage({
          id: url,
          url,
          preview: null,
          source: this.source
        });
      });
    }
    stripTrailingSlash(url) {
      return url.endsWith("/") ? url.slice(0, -1) : url;
    }
    lastSegment(url) {
      const clean = this.stripTrailingSlash(url);
      const i = clean.lastIndexOf("/");
      return i >= 0 ? clean.slice(i + 1) : clean;
    }
  };

  // src/mangaworld.js
  var MangaWorldParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 16) {
      super(context, source, domain, pageSize);
      this.wwwForce = true;
      this.archivePath = "/archive";
      this.selectMangaEntry = ".comics-grid .entry";
      this.selectEntryLink = "a.thumb";
      this.selectEntryTitle = ".name a.manga-title";
      this.selectEntryCover = ".thumb img";
      this.selectEntryAuthor = ".author a";
      this.selectEntryStatus = ".status a";
      this.selectEntryGenres = ".genres a[href*='genre=']";
      this.selectAltTitle = ".meta-data .font-weight-bold";
      this.altTitleLabel = "Titoli alternativi";
      this.descriptionId = "noidungm";
      this.selectChapterWrap = ".chapters-wrapper .chapter a";
      this.selectChapterTitle = "span.d-inline-block";
      this.selectChapterDate = ".chap-date";
      this.stylePage = "?style=list";
      this.selectWebtoonPages = "img.page-image";
      this.selectMangaPages = "#page img.img-fluid";
      this.selectMangaPagesFallback = "img.img-fluid";
      this.statusMap = {
        "in corso": MangaState.ONGOING,
        "finito": MangaState.FINISHED,
        "droppato": MangaState.ABANDONED,
        "in pausa": MangaState.PAUSED
      };
      this.monthNames = {
        "gennaio": 0,
        "febbraio": 1,
        "marzo": 2,
        "aprile": 3,
        "maggio": 4,
        "giugno": 5,
        "luglio": 6,
        "agosto": 7,
        "settembre": 8,
        "ottobre": 9,
        "novembre": 10,
        "dicembre": 11
      };
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // Canonical request host. Prepends "www." for bare apex domains because the
    // apex 301 isn't reliably followed by every fetch implementation.
    requestHost() {
      const host = (this.domain || "").replace(/^https?:\/\//, "").replace(/\/.*$/, "");
      if (this.wwwForce && host && !host.startsWith("www.") && host.split(".").length === 2) {
        return `www.${host}`;
      }
      return host;
    }
    // Absolute URL on the canonical request host (for fetches). Storage URLs are
    // still kept relative via this.toRelativeUrl(...).
    reqUrl(relOrAbs) {
      if (!relOrAbs)
        return `https://${this.requestHost()}/`;
      const rel = this.toRelativeUrl(relOrAbs);
      return `https://${this.requestHost()}${rel.startsWith("/") ? "" : "/"}${rel}`;
    }
    // "31 Maggio 2026" -> epoch millis (best-effort; 0 if unparseable).
    parseDate(text) {
      if (!text)
        return 0;
      const m = text.trim().toLowerCase().match(/(\d{1,2})\s+([a-zàèéìòù]+)\s+(\d{4})/);
      if (!m)
        return 0;
      const day = parseInt(m[1], 10);
      const month = this.monthNames[m[2]];
      const year = parseInt(m[3], 10);
      if (month === void 0 || isNaN(day) || isNaN(year))
        return 0;
      return Date.UTC(year, month, day);
    }
    sortParam(order) {
      switch (order) {
        case SortOrder.POPULARITY:
          return "most_read";
        case SortOrder.POPULARITY_ASC:
          return "less_read";
        case SortOrder.ALPHABETICAL:
          return "a-z";
        case SortOrder.ALPHABETICAL_DESC:
          return "z-a";
        case SortOrder.NEWEST:
          return "newest";
        case SortOrder.NEWEST_ASC:
          return "oldest";
        default:
          return "a-z";
      }
    }
    stateParam(state) {
      switch (state) {
        case MangaState.ONGOING:
          return "ongoing";
        case MangaState.FINISHED:
          return "completed";
        case MangaState.ABANDONED:
          return "dropped";
        case MangaState.PAUSED:
          return "paused";
        default:
          return null;
      }
    }
    async getListPage(page, order, filter = {}) {
      const query = filter.query || "";
      const tags = filter.tags || [];
      const states = filter.states || [];
      const noFilters = !query && tags.length === 0 && states.length === 0;
      if (order === SortOrder.UPDATED && noFilters) {
        const html2 = await this.context.httpGet(`https://${this.requestHost()}/?page=${page}`, this);
        return this.parseMangaList(html2);
      }
      let url = `https://${this.requestHost()}${this.archivePath}?&page=${page}`;
      if (query)
        url += `&keyword=${encodeURIComponent(query)}`;
      for (const t of tags) {
        const key = t && (t.key || t.title || t);
        if (key)
          url += `&genre=${encodeURIComponent(key)}`;
      }
      url += `&sort=${this.sortParam(order)}`;
      for (const s of states) {
        const sp = this.stateParam(s);
        if (sp)
          url += `&status=${sp}`;
      }
      if (filter.year)
        url += `&year=${filter.year}`;
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const entries = this.queryAll(doc, [
        this.selectMangaEntry,
        ".comics-grid .entry",
        ".comics-grid .comic",
        ".entry"
      ]);
      const list = [];
      for (const div of entries) {
        const a = div.querySelector(this.selectEntryLink) || div.querySelector("a.thumb") || div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const titleEl = div.querySelector(this.selectEntryTitle) || div.querySelector(".name a") || div.querySelector(".manga-title");
        const img = div.querySelector(this.selectEntryCover) || div.querySelector("img");
        const authorEl = div.querySelector(this.selectEntryAuthor);
        const tags = [];
        try {
          for (const g of div.querySelectorAll(this.selectEntryGenres)) {
            const gh = g.getAttribute("href") || "";
            const key = (gh.split("genre=")[1] || "").split("&")[0];
            const title = (g.textContent || "").trim();
            if (key && title)
              tags.push({ key: decodeURIComponent(key), title });
          }
        } catch {
        }
        let state;
        const statusText = div.querySelector(this.selectEntryStatus)?.textContent?.trim()?.toLowerCase();
        if (statusText)
          state = this.statusMap[statusText];
        list.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title: (titleEl?.textContent || a.getAttribute("title") || "").trim(),
          altTitles: [],
          tags,
          authors: authorEl ? [authorEl.textContent.trim()].filter(Boolean) : [],
          state,
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return list;
    }
    async getDetails(manga) {
      const html = await this.context.httpGet(this.reqUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const altTitles = [];
      try {
        for (const label of doc.querySelectorAll(this.selectAltTitle)) {
          if ((label.textContent || "").includes(this.altTitleLabel)) {
            const parent = label.parentElement;
            if (parent) {
              let own = parent.textContent || "";
              for (const c of parent.children)
                own = own.replace(c.textContent, "");
              const value = own.split(":").pop().trim();
              if (value)
                altTitles.push(value);
            }
            break;
          }
        }
      } catch {
      }
      const description = doc.getElementById(this.descriptionId)?.textContent?.trim() || manga.description || "";
      const chapterEls = this.queryAll(doc, [
        this.selectChapterWrap,
        ".chapters-wrapper .chapter a",
        ".chapters-wrapper a.chap",
        ".chapter a"
      ]).reverse();
      const chapters = chapterEls.map((a, i) => {
        const href = a.getAttribute("href");
        if (!href)
          return null;
        const absUrl = this.toAbsoluteUrl(href);
        const relUrl = this.toRelativeUrl(absUrl);
        const titleEl = a.querySelector(this.selectChapterTitle);
        const dateEl = a.querySelector(this.selectChapterDate);
        return new MangaChapter({
          id: relUrl,
          url: relUrl + this.stylePage,
          title: (titleEl?.textContent || a.getAttribute("title") || "").trim() || null,
          number: i + 1,
          volume: 0,
          branch: null,
          scanlator: null,
          uploadDate: this.parseDate(dateEl?.textContent),
          source: this.source
        });
      }).filter(Boolean);
      return new Manga({
        ...manga,
        altTitles: altTitles.length ? altTitles : manga.altTitles,
        description,
        chapters
      });
    }
    async getPages(chapter) {
      const html = await this.context.httpGet(this.reqUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      let imgs = Array.from(doc.querySelectorAll(this.selectWebtoonPages));
      if (!imgs.length) {
        imgs = this.queryAll(doc, [this.selectMangaPages, "#page img.img-fluid", this.selectMangaPagesFallback]);
      }
      const pages = [];
      for (const img of imgs) {
        const abs = this.imageSrc(img);
        if (!abs)
          continue;
        if (/\/(assets|svg|logo)/i.test(abs) && !/\/chapters?\//i.test(abs))
          continue;
        pages.push(new MangaPage({
          id: abs,
          url: abs,
          preview: null,
          source: this.source
        }));
      }
      return pages;
    }
  };

  // src/mangadventure.js
  var MangAdventureParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 25) {
      super(context, source, domain, pageSize);
      this.apiPath = "api/v2";
      this.seriesPath = "series";
      this.chaptersPath = "chapters";
      this.pagesPath = "pages";
      this.categoriesPath = "categories";
      this.readerPrefix = "/reader/";
    }
    apiUrl(...segments) {
      const base = this.domain.startsWith("http") ? this.domain : `https://${this.domain}`;
      const path = [this.apiPath, ...segments].filter((s) => s !== void 0 && s !== null && s !== "").map((s) => String(s).replace(/^\/+|\/+$/g, "")).join("/");
      return `${base.replace(/\/+$/, "")}/${path}`;
    }
    async getJson(url) {
      const text = await this.context.httpGet(url, this);
      return JSON.parse(text);
    }
    // Recover the API slug from a manga url like `/reader/arc-light/`.
    slugOf(manga) {
      let url = manga && manga.url ? String(manga.url) : "";
      if (manga && manga.slug)
        return manga.slug;
      url = this.toRelativeUrl(url);
      if (url.startsWith(this.readerPrefix)) {
        url = url.slice(this.readerPrefix.length);
      } else {
        url = url.replace(/^\/+/, "");
      }
      return url.replace(/\/+$/, "");
    }
    sortParam(order) {
      switch (order) {
        case SortOrder.ALPHABETICAL:
          return "title";
        case SortOrder.ALPHABETICAL_DESC:
          return "-title";
        case SortOrder.UPDATED:
          return "-latest_upload";
        case SortOrder.POPULARITY:
          return "-views";
        default:
          return "-latest_upload";
      }
    }
    statusParam(state) {
      switch (state) {
        case MangaState.ONGOING:
          return "ongoing";
        case MangaState.FINISHED:
          return "completed";
        case MangaState.ABANDONED:
          return "canceled";
        case MangaState.PAUSED:
          return "hiatus";
        default:
          return "any";
      }
    }
    mapState(status) {
      switch (String(status || "").toLowerCase()) {
        case "ongoing":
          return MangaState.ONGOING;
        case "completed":
          return MangaState.FINISHED;
        case "canceled":
          return MangaState.ABANDONED;
        case "hiatus":
          return MangaState.PAUSED;
        default:
          return void 0;
      }
    }
    get contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    // Build a Manga from a `results[]` entry of the series listing/search.
    mangaFromJson(it) {
      if (it == null || it.chapters === null)
        return null;
      const path = it.url || `${this.readerPrefix}${it.slug}/`;
      return new Manga({
        id: path,
        url: path,
        slug: it.slug,
        publicUrl: this.toAbsoluteUrl(path),
        coverUrl: it.cover || "",
        largeCoverUrl: it.cover || "",
        title: it.title || "",
        source: this.source,
        contentRating: this.contentRating
      });
    }
    async getListPage(page, order, filter) {
      filter = filter || {};
      const params = new URLSearchParams();
      params.set("limit", String(this.pageSize));
      params.set("page", String(page));
      if (filter.query)
        params.set("title", filter.query);
      const tags = Array.isArray(filter.tags) ? filter.tags : [];
      const tagsExclude = Array.isArray(filter.tagsExclude) ? filter.tagsExclude : [];
      if (tags.length || tagsExclude.length) {
        const inc = tags.map((t) => t.key).join(",");
        const exc = tagsExclude.map((t) => `-${t.key}`).join(",");
        params.set("categories", `${inc},${exc}`);
      }
      const states = Array.isArray(filter.states) ? filter.states : [];
      if (states.length === 1) {
        params.set("status", this.statusParam(states[0]));
      }
      params.set("sort", this.sortParam(order));
      let json;
      try {
        json = await this.getJson(`${this.apiUrl(this.seriesPath)}?${params.toString()}`);
      } catch (e) {
        if (/HTTP\s*404/.test(String(e && e.message)))
          return [];
        throw e;
      }
      const results = json && Array.isArray(json.results) ? json.results : [];
      return results.map((it) => this.mangaFromJson(it)).filter(Boolean);
    }
    async getDetails(manga) {
      const slug = this.slugOf(manga);
      const seriesUrl = `${this.apiUrl(this.seriesPath, slug)}`;
      const chaptersUrl = `${this.apiUrl(this.seriesPath, slug, this.chaptersPath)}?date_format=timestamp`;
      const details = await this.getJson(seriesUrl);
      let chaptersJson = { results: [] };
      try {
        chaptersJson = await this.getJson(chaptersUrl);
      } catch {
        chaptersJson = { results: [] };
      }
      const authors = Array.isArray(details.authors) ? details.authors : [];
      const artists = Array.isArray(details.artists) ? details.artists : [];
      const allAuthors = [...authors, ...artists].filter(Boolean);
      const categories = Array.isArray(details.categories) ? details.categories : [];
      const tags = categories.map((name) => ({ title: name, key: name }));
      const aliases = Array.isArray(details.aliases) ? details.aliases : [];
      const rawChapters = chaptersJson && Array.isArray(chaptersJson.results) ? chaptersJson.results.slice() : [];
      rawChapters.reverse();
      const chapters = rawChapters.map((it, i) => {
        const groups = Array.isArray(it.groups) ? it.groups.filter(Boolean) : [];
        const published = it.published != null ? Number(it.published) : 0;
        return new MangaChapter({
          // API chapter id is required to fetch pages — keep it numeric.
          id: it.id,
          url: it.url,
          title: it.full_title || it.title || "",
          number: typeof it.number === "number" ? it.number : parseFloat(it.number) || 0,
          volume: it.volume != null ? parseInt(it.volume, 10) || 0 : 0,
          branch: null,
          scanlator: groups.join(", "),
          uploadDate: Number.isFinite(published) ? published : 0,
          index: i,
          source: this.source
        });
      });
      return new Manga({
        ...manga,
        title: details.title || manga.title,
        slug,
        description: details.description || "",
        altTitles: aliases,
        authors: allAuthors,
        tags,
        coverUrl: details.cover || manga.coverUrl || "",
        largeCoverUrl: details.cover || manga.largeCoverUrl || manga.coverUrl || "",
        state: this.mapState(details.status),
        contentRating: this.contentRating,
        source: this.source,
        chapters
      });
    }
    async getPages(chapter) {
      const id = chapter && chapter.id;
      if (id == null || id === "")
        return [];
      const url = `${this.apiUrl(this.chaptersPath, id, this.pagesPath)}?track=true`;
      let json;
      try {
        json = await this.getJson(url);
      } catch {
        return [];
      }
      const results = json && Array.isArray(json.results) ? json.results : [];
      return results.map((it) => new MangaPage({
        id: it.image || String(it.id),
        url: it.image,
        preview: null,
        source: this.source
      })).filter((p) => p.url);
    }
    // Optional: category list, exposed in case the client wants tag filters.
    async getTags() {
      try {
        const json = await this.getJson(this.apiUrl(this.categoriesPath));
        const results = json && Array.isArray(json.results) ? json.results : [];
        return results.map((it) => ({ title: it.name, key: it.name }));
      } catch {
        return [];
      }
    }
  };

  // src/initmanga.js
  var InitMangaParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 20) {
      super(context, source, domain, pageSize);
      this.mangaUrlDirectory = "seri";
      this.popularUrlSlug = "seri";
      this.latestUrlSlug = "son-guncellemeler";
      this.searchPath = "/wp-json/initlise/v1/search";
      this.chapterPagePath = "bolum";
      this.selectListPanels = "div.manga-item-grid > div.uk-panel";
      this.selectListPanelsFallback = "div.uk-panel";
      this.selectTitle = "#manga-title";
      this.selectDescription = "#manga-description";
      this.selectCover = "div.story-cover-wrap img, a.story-cover img";
      this.selectTags = "#genre-tags a";
      this.selectChapterItem = "div.chapter-item";
      this.selectChapterTitle = "h3, h4";
      this.selectChapterContentImg = "#chapter-content img";
      this.maxProbedPages = 500;
      this.probeBatchSize = 8;
    }
    // ---- helpers (same conventions as madara.js) -------------------------
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    get contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    // ---- list ------------------------------------------------------------
    async getListPage(page, order, filter = {}) {
      const query = filter && filter.query;
      const tags = filter && filter.tags || [];
      if (query) {
        return this.search(page, query);
      }
      if (tags.length) {
        return this.getGenrePage(page, tags[0]);
      }
      if (order === SortOrder.UPDATED) {
        return this.getDirectoryPage(page, this.latestUrlSlug, true);
      }
      return this.getDirectoryPage(page, this.popularUrlSlug, false);
    }
    async getDirectoryPage(page, slug, alwaysPaged) {
      let url = `https://${this.domain}/${String(slug).replace(/^\/+|\/+$/g, "")}/`;
      if (alwaysPaged || page > 1) {
        url += `page/${page}/`;
      }
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(this.context.parseHTML(html));
    }
    async getGenrePage(page, tag) {
      if (!tag || !tag.key)
        return [];
      const base = this.toAbsoluteUrl(tag.key).replace(/\/+$/, "");
      const url = page > 1 ? `${base}/page/${page}/` : `${base}/`;
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(this.context.parseHTML(html));
    }
    async search(page, query) {
      const url = `https://${this.domain}${this.searchPath}?term=${encodeURIComponent(query)}&page=${page}`;
      let raw;
      try {
        raw = await this.context.httpGet(url, this);
      } catch {
        raw = "";
      }
      if (!raw || !raw.trim())
        return [];
      if (raw.trimStart().startsWith("<")) {
        return this.parseMangaList(this.context.parseHTML(raw));
      }
      let list;
      try {
        list = JSON.parse(raw);
      } catch {
        return [];
      }
      if (!Array.isArray(list))
        return [];
      return list.map((json) => {
        const fullUrl = json && json.url || "";
        if (!fullUrl)
          return null;
        const relativeUrl = this.toRelativeUrl(fullUrl);
        const title = this.stripHtml(json.title || "").trim();
        return new Manga({
          id: relativeUrl,
          url: relativeUrl,
          publicUrl: fullUrl,
          coverUrl: json.thumb || "",
          title,
          source: this.source,
          contentRating: this.contentRating
        });
      }).filter(Boolean);
    }
    stripHtml(s) {
      return String(s).replace(/<[^>]*>/g, "").replace(/&amp;/g, "&").replace(/&#0?39;/g, "'").replace(/&quot;/g, '"');
    }
    parseMangaList(doc) {
      let panels = Array.from(doc.querySelectorAll(this.selectListPanels));
      if (!panels.length)
        panels = Array.from(doc.querySelectorAll(this.selectListPanelsFallback));
      panels = panels.filter((panel) => {
        if (panel.classList && panel.classList.contains("manga-item-ranking"))
          return false;
        let parent = panel.parentElement;
        while (parent) {
          const id = parent.id || "";
          const cls = parent.className || "";
          if (id === "im-sidebar" || /\bsidebar-widget\b/.test(cls) || /\btop-manga-widget\b/.test(cls)) {
            return false;
          }
          parent = parent.parentElement;
        }
        return true;
      });
      const seen = /* @__PURE__ */ new Set();
      const out = [];
      for (const panel of panels) {
        const link = this.findSeriesLink(panel);
        if (!link)
          continue;
        const href = link.getAttribute("href");
        const relativeUrl = href ? this.toRelativeUrl(href) : "";
        if (!relativeUrl)
          continue;
        const title = this.extractSeriesTitle(panel, link);
        if (!title)
          continue;
        if (seen.has(relativeUrl))
          continue;
        seen.add(relativeUrl);
        const adult = (panel.textContent || "").includes("18+");
        out.push(new Manga({
          id: relativeUrl,
          url: relativeUrl,
          publicUrl: this.toAbsoluteUrl(href),
          coverUrl: this.imageSrc(panel.querySelector("img")),
          title,
          source: this.source,
          contentRating: this.source && this.source.isNsfw ? ContentRating.ADULT : adult ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return out;
    }
    findSeriesLink(panel) {
      const dir = this.mangaUrlDirectory;
      return Array.from(panel.querySelectorAll("a[href]")).find((a) => {
        const href = a.getAttribute("href") || "";
        return href.includes(`/${dir}/`) && !href.includes(`/${dir}/page/`) && !href.includes("/bolum");
      }) || null;
    }
    extractSeriesTitle(panel, link) {
      const titleEl = panel.querySelector(
        "h2 a, h2, h3 a, h3, h4 a, h4, a.uk-link-heading, strong.slider-title, strong.uk-h2"
      );
      const fromEl = titleEl && titleEl.textContent.trim();
      if (fromEl)
        return fromEl;
      const fromAttr = (link.getAttribute("title") || "").trim();
      if (fromAttr)
        return fromAttr;
      return (link.textContent || "").trim();
    }
    // ---- details ---------------------------------------------------------
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const title = (doc.querySelector(this.selectTitle)?.textContent || "").trim() || manga.title;
      let description = "";
      const descEl = doc.querySelector(this.selectDescription);
      if (descEl) {
        const clone = descEl.cloneNode(true);
        clone.querySelectorAll("a, span").forEach((el) => el.remove());
        description = clone.textContent.trim();
      }
      const cover = this.imageSrc(doc.querySelector(this.selectCover)) || manga.coverUrl;
      const tags = Array.from(doc.querySelectorAll(this.selectTags)).map((a) => ({
        title: (a.textContent || "").trim().replace(/^#/, ""),
        key: this.toRelativeUrl(a.getAttribute("href"))
      })).filter((t) => t.title && t.key);
      const chapters = await this.fetchChapters(manga.url, doc);
      return new Manga({
        ...manga,
        title,
        description,
        coverUrl: cover,
        largeCoverUrl: cover || manga.largeCoverUrl || manga.coverUrl,
        tags: tags.length ? tags : manga.tags,
        contentRating: manga.contentRating === ContentRating.ADULT ? ContentRating.ADULT : this.contentRating,
        source: this.source,
        chapters
      });
    }
    async fetchChapters(mangaUrl, firstDoc) {
      const base = this.toAbsoluteUrl(mangaUrl).replace(/\/+$/, "");
      const collected = [];
      const seenUrls = /* @__PURE__ */ new Set();
      let page = 1;
      let doc = firstDoc;
      while (page <= 200) {
        if (page > 1) {
          const url = `${base}/${this.chapterPagePath}/page/${page}/`;
          let html;
          try {
            html = await this.context.httpGet(url, this);
          } catch {
            break;
          }
          doc = this.context.parseHTML(html);
        }
        const items = Array.from(doc.querySelectorAll(this.selectChapterItem));
        if (!items.length)
          break;
        const before = seenUrls.size;
        for (const el of items) {
          const ch = this.parseChapter(el);
          if (!ch || seenUrls.has(ch.url))
            continue;
          seenUrls.add(ch.url);
          collected.push(ch);
        }
        if (seenUrls.size === before)
          break;
        page++;
      }
      collected.sort((a, b) => {
        const aUnknown = a.number <= 0 ? 1 : 0;
        const bUnknown = b.number <= 0 ? 1 : 0;
        if (aUnknown !== bUnknown)
          return aUnknown - bUnknown;
        if (a.number !== b.number)
          return a.number - b.number;
        const ad = a.uploadDate || Number.MAX_SAFE_INTEGER;
        const bd = b.uploadDate || Number.MAX_SAFE_INTEGER;
        if (ad !== bd)
          return ad - bd;
        return (a.title || "").toLowerCase().localeCompare((b.title || "").toLowerCase());
      });
      return collected.map((ch, i) => new MangaChapter({ ...ch, index: i }));
    }
    parseChapter(el) {
      const a = el.querySelector("a[href]");
      if (!a)
        return null;
      const url = this.toRelativeUrl(a.getAttribute("href"));
      if (!url)
        return null;
      let rawTitle = (el.querySelector(this.selectChapterTitle)?.textContent || "").trim();
      if (!rawTitle)
        rawTitle = (a.getAttribute("title") || "").trim();
      if (!rawTitle)
        rawTitle = (el.querySelector("img[alt]")?.getAttribute("alt") || "").trim();
      if (!rawTitle)
        rawTitle = (a.textContent || "").trim();
      const number = this.extractChapterNumber(url, rawTitle);
      const title = rawTitle || (number != null ? `B\xF6l\xFCm ${this.chapterNumberToString(number)}` : "");
      return {
        id: url,
        url,
        title,
        number: number != null ? number : 0,
        volume: 0,
        scanlator: null,
        uploadDate: this.parseChapterDate(el),
        branch: null,
        source: this.source
      };
    }
    extractChapterNumber(url, rawTitle) {
      const re = /(?:bolum|bölüm|chapter|ch)[^0-9]*([0-9]+(?:[.,][0-9]+)?)/i;
      const lastSeg = url.split("/").filter(Boolean).pop() || url;
      let m = lastSeg.match(re);
      if (!m && rawTitle)
        m = rawTitle.match(re);
      if (!m)
        return null;
      const n = parseFloat(m[1].replace(",", "."));
      return Number.isFinite(n) ? n : null;
    }
    chapterNumberToString(n) {
      return String(n).replace(/\.0$/, "");
    }
    parseChapterDate(el) {
      const dt = el.querySelector("time")?.getAttribute("datetime");
      if (dt) {
        const t = Date.parse(dt);
        if (!Number.isNaN(t))
          return t;
      }
      const tip = el.querySelector("span[uk-tooltip]")?.getAttribute("uk-tooltip");
      if (tip) {
        const title = tip.split("title:")[1];
        if (title) {
          const t = this.parseTurkishDate(title.split(";")[0].trim());
          if (t)
            return t;
        }
      }
      return 0;
    }
    parseTurkishDate(s) {
      const months = {
        ocak: 0,
        "\u015Fubat": 1,
        mart: 2,
        nisan: 3,
        "may\u0131s": 4,
        haziran: 5,
        temmuz: 6,
        "a\u011Fustos": 7,
        "eyl\xFCl": 8,
        ekim: 9,
        "kas\u0131m": 10,
        "aral\u0131k": 11
      };
      const m = s.match(/(\d{1,2})\s+(\S+)\s+(\d{4})(?:\s+(\d{1,2}):(\d{2}))?/);
      if (!m)
        return 0;
      const month = months[m[2].toLowerCase()];
      if (month == null)
        return 0;
      const d = new Date(Number(m[3]), month, Number(m[1]), Number(m[4] || 0), Number(m[5] || 0));
      const t = d.getTime();
      return Number.isNaN(t) ? 0 : t;
    }
    // ---- pages -----------------------------------------------------------
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      let urls = Array.from(doc.querySelectorAll(this.selectChapterContentImg)).map((img) => this.imageSrc(img)).filter(Boolean);
      urls = this.dedupe(urls);
      if (urls.length) {
        return (await this.maybeExpandSequentialPages(urls)).map((u) => this.toMangaPage(u));
      }
      const re = /https?:\/\/[^\s"'<>]+\/wp-content\/uploads\/init-manga\/[^\s"'<>]+/g;
      urls = this.dedupe(Array.from(html.matchAll(re)).map((mm) => mm[0]));
      if (urls.length) {
        return (await this.maybeExpandSequentialPages(urls)).map((u) => this.toMangaPage(u));
      }
      const decrypted = this.decryptChapter(html, doc);
      if (decrypted && decrypted.length) {
        return (await this.maybeExpandSequentialPages(decrypted)).map((u) => this.toMangaPage(u));
      }
      return [];
    }
    /**
     * GAP (documented honestly): InitManga chapters whose pages are NOT exposed
     * as a sequential 0001.jpg seed embed them in an `InitMangaEncryptedChapter`
     * JSON blob, AES-CBC encrypted with a PBKDF2(SHA-512, 999 iters, 256-bit)
     * key whose passphrase is base64 inside an external `init-main-js-extra`
     * script. Reproducing that requires AES + PBKDF2 + fetching/decoding the key
     * script — out of scope for this fetch+DOMParser port. On the live source
     * (merlintoon.com) the sequential fast-path (paths 1/2 above) already yields
     * complete page lists, so this branch is intentionally a no-op stub.
     */
    decryptChapter() {
      return null;
    }
    /**
     * If the chapter exposes a single sequential seed image
     * (.../init-manga/<id>/<chapter>/<NNNN>.<ext>), probe forward (0002, 0003, ...)
     * until a page is missing, building the full list. Mirrors the Kotlin
     * `maybeExpandSequentialPages` + `doesPageExist` HEAD-probe logic, but uses
     * httpGet (the harness context has no httpHead) and treats a thrown
     * non-2xx / HTML body as "missing".
     */
    async maybeExpandSequentialPages(urls) {
      if (urls.length !== 1)
        return urls;
      const seed = urls[0];
      const m = seed.match(/^(https?:\/\/[^\s"'<>]+\/wp-content\/uploads\/init-manga\/.*\/)(\d+)(\.[A-Za-z0-9]+)$/);
      if (!m)
        return urls;
      const prefix = m[1];
      const startStr = m[2];
      const suffix = m[3];
      const pad = startStr.length;
      const start = parseInt(startStr, 10);
      if (!Number.isFinite(start))
        return urls;
      const expanded = [];
      let n = start;
      let stop = false;
      while (!stop && n <= this.maxProbedPages) {
        const batch = [];
        for (let i = 0; i < this.probeBatchSize && n + i <= this.maxProbedPages; i++) {
          const num = String(n + i).padStart(pad, "0");
          batch.push(prefix + num + suffix);
        }
        const results = await Promise.all(batch.map((u) => this.doesPageExist(u)));
        for (let i = 0; i < batch.length; i++) {
          if (results[i]) {
            expanded.push(batch[i]);
          } else {
            stop = true;
            break;
          }
        }
        n += batch.length;
      }
      return expanded.length ? expanded : urls;
    }
    async doesPageExist(url) {
      try {
        const body = await this.context.httpGet(url, this);
        if (typeof body === "string") {
          const head = body.slice(0, 200).trimStart().toLowerCase();
          if (head.startsWith("<!doctype") || head.startsWith("<html"))
            return false;
        }
        return true;
      } catch {
        return false;
      }
    }
    dedupe(arr) {
      return Array.from(new Set(arr));
    }
    toMangaPage(url) {
      return new MangaPage({ id: url, url, source: this.source });
    }
  };

  // src/fuzzydoodle.js
  var FuzzyDoodleParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 24) {
      super(context, source, domain, pageSize);
      this.listPath = "/manga";
      this.ongoingValue = "ongoing";
      this.finishedValue = "completed";
      this.pausedValue = "haitus";
      this.abandonedValue = "dropped";
      this.mangaValue = "manga";
      this.manhwaValue = "manhwa";
      this.manhuaValue = "manhua";
      this.comicsValue = "bande-dessinee";
      this.selectMangas = "div#card-real";
      this.selectMangaTitle = "h2";
      this.selectAltTitle = "Alternative Titles:";
      this.selectState = "a[href*=status] span";
      this.selectAuthorLabels = ["Auteur", "Author", "\u0627\u0644\u0645\u0624\u0644\u0641"];
      this.selectDescription = ["p#description", "div:has(> p#description) p", "#description"];
      this.selectTagManga = "a[href*=genre]";
      this.selectChapters = "div#chapters-list > a[href]";
      this.selectChapterName = ["div.gap-2", "#item-title"];
      this.selectChapterDate = ["span.text-gray-500", "div.gap-3 > span", "div:has(#item-title) span.mt-1"];
      this.selectPagination = "ul.pagination li[onclick]";
      this.selectPages = ["div#chapter-container img", "img.chapter-image"];
      this.ongoing = /* @__PURE__ */ new Set(["en cours", "ongoing", "\u0645\u0633\u062A\u0645\u0631"]);
      this.finished = /* @__PURE__ */ new Set(["termin\xE9", "dropped", "cancelled", "\u0645\u062A\u0648\u0642\u0641", "\u0645\u0643\u062A\u0645\u0644"]);
      this.abandoned = /* @__PURE__ */ new Set(["canceled", "cancelled", "dropped", "abandonn\xE9", "\u0645\u062A\u0648\u0642\u0641"]);
      this.paused = /* @__PURE__ */ new Set(["hiatus", "on hold", "en pause", "en attente"]);
    }
    // ---- helpers (mirrors madara.js conventions) -------------------------
    queryAll(doc, selectors) {
      for (const selector of (selectors || []).filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    queryFirst(node, selectors) {
      for (const selector of (selectors || []).filter(Boolean)) {
        try {
          const el = node.querySelector(selector);
          if (el)
            return el;
        } catch {
        }
      }
      return null;
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    text(el) {
      return el ? el.textContent.replace(/\s+/g, " ").trim() : "";
    }
    // Some FuzzyDoodle hosts (e.g. lelscanfr.com) do an apex -> www host-level
    // 301. Node/undici's redirect:'follow' is flaky against that particular
    // Cloudflare config and intermittently surfaces the bare 301. To stay
    // deterministic we issue the request, and on any failure retry once against
    // the `www.` (or de-`www.`) variant, pinning `this.domain` to whatever host
    // actually serves 200 so every later request targets the canonical host.
    async httpGetFollow(url) {
      try {
        return await this.context.httpGet(url, this);
      } catch (e) {
        let alt = null;
        try {
          const u = new URL(url);
          if (u.hostname.startsWith("www.")) {
            u.hostname = u.hostname.slice(4);
          } else {
            u.hostname = `www.${u.hostname}`;
          }
          alt = u;
        } catch {
          throw e;
        }
        const html = await this.context.httpGet(alt.href, this);
        this.domain = alt.hostname;
        return html;
      }
    }
    // ---- list / search ---------------------------------------------------
    buildListUrl(page, filter) {
      const f = filter || {};
      let url = `https://${this.domain}${this.listPath}?page=${page}`;
      url += "&type=";
      if (f.query)
        url += `&title=${encodeURIComponent(f.query)}`;
      url += "&status=";
      const states = f.states || [];
      const state = Array.isArray(states) ? states[0] : states;
      if (state) {
        let sv = "";
        switch (state) {
          case MangaState.ONGOING:
            sv = this.ongoingValue;
            break;
          case MangaState.FINISHED:
            sv = this.finishedValue;
            break;
          case MangaState.PAUSED:
            sv = this.pausedValue;
            break;
          case MangaState.ABANDONED:
            sv = this.abandonedValue;
            break;
        }
        url += sv;
      }
      const tags = f.tags || [];
      for (const t of tags) {
        const key = t && (t.key !== void 0 ? t.key : t) || "";
        url += `&${encodeURIComponent("genre[]")}=${key}`;
      }
      return url;
    }
    async getListPage(page, order, filter) {
      const url = this.buildListUrl(page, filter);
      const html = await this.httpGetFollow(url);
      return this.parseMangaList(this.context.parseHTML(html));
    }
    parseMangaList(doc) {
      const cards = this.queryAll(doc, [this.selectMangas, "div#card-real", "div[id=card-real]"]);
      const list = [];
      for (const div of cards) {
        const a = div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const img = div.querySelector("img");
        const titleEl = this.queryFirst(div, [this.selectMangaTitle, "h2", "h3"]);
        list.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(href),
          coverUrl: this.imageSrc(img),
          title: this.text(titleEl) || (img && img.getAttribute("alt") || "").trim(),
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return list;
    }
    // ---- details ---------------------------------------------------------
    // Finds the value <span> next to a label <span> whose text matches one of
    // `labels`. Replaces Kotlin's `p:contains(Auteur) span` (no :contains in DOM).
    findLabeledValue(doc, labels) {
      const wanted = labels.map((l) => l.toLowerCase());
      for (const p of Array.from(doc.querySelectorAll("p, div.flex, div"))) {
        const spans = Array.from(p.querySelectorAll(":scope > span, :scope > a > span, span"));
        if (spans.length < 2)
          continue;
        const labelTxt = this.text(spans[0]).toLowerCase();
        if (wanted.some((w) => labelTxt.includes(w))) {
          const val = this.text(spans[spans.length - 1]);
          if (val && val.toLowerCase() !== labelTxt)
            return val;
        }
      }
      return "";
    }
    parseState(doc) {
      const stateEl = this.queryFirst(doc, [this.selectState, "a[href*=status] span", "a[href*='status'] span"]);
      const s = this.text(stateEl).toLowerCase();
      if (!s)
        return void 0;
      if (this.ongoing.has(s))
        return MangaState.ONGOING;
      if (this.finished.has(s))
        return MangaState.FINISHED;
      if (this.abandoned.has(s))
        return MangaState.ABANDONED;
      if (this.paused.has(s))
        return MangaState.PAUSED;
      const has = (set) => Array.from(set).some((k) => s.includes(k));
      if (has(this.ongoing))
        return MangaState.ONGOING;
      if (has(this.finished))
        return MangaState.FINISHED;
      if (has(this.abandoned))
        return MangaState.ABANDONED;
      if (has(this.paused))
        return MangaState.PAUSED;
      return void 0;
    }
    parseAltTitle(doc) {
      const label = (this.selectAltTitle || "").toLowerCase();
      for (const block of Array.from(doc.querySelectorAll("div.flex, div"))) {
        const spans = Array.from(block.querySelectorAll(":scope > span"));
        if (spans.length < 2)
          continue;
        if (this.text(spans[0]).toLowerCase().includes(label)) {
          return this.text(spans[spans.length - 1]);
        }
      }
      return "";
    }
    parseTitle(doc) {
      const h = doc.querySelector("h2.text-2xl, h2.font-bold, h1");
      if (!h)
        return "";
      let t = "";
      for (const node of Array.from(h.childNodes)) {
        if (node.nodeType === 3)
          t += node.textContent;
      }
      t = t.replace(/\s+/g, " ").trim();
      if (t)
        return t;
      return this.text(h).replace(/\s*\[[^\]]*\]\s*$/, "").trim();
    }
    // Reads the synopsis. The site emits <p id="description"><p>...</p></p>,
    // which the HTML5 parser flattens (a <p> can't nest a <p>), leaving
    // #description empty and the real text in following-sibling <p> nodes.
    parseDescription(doc) {
      const el = this.queryFirst(doc, this.selectDescription);
      if (!el)
        return "";
      if (el.innerHTML && el.innerHTML.trim())
        return el.innerHTML;
      const parts = [];
      let sib = el.nextElementSibling;
      while (sib && sib.tagName === "P") {
        if (sib.id && sib.id !== el.id)
          break;
        parts.push(sib.innerHTML);
        sib = sib.nextElementSibling;
      }
      if (parts.length)
        return parts.join("");
      return el.parentElement ? el.parentElement.innerHTML : "";
    }
    parseTags(doc) {
      const els = this.queryAll(doc, [this.selectTagManga, "a[href*=genre]", "div.flex > a.inline-block"]);
      const seen = /* @__PURE__ */ new Set();
      const tags = [];
      for (const a of els) {
        const href = a.getAttribute("href") || "";
        const m = href.match(/genre(?:\[\])?=([^&]+)/);
        const key = m ? decodeURIComponent(m[1]) : href.split("=").pop();
        const title = this.text(a) || key;
        if (!key || seen.has(key))
          continue;
        seen.add(key);
        tags.push({ key, title, source: this.source });
      }
      return tags;
    }
    maxChapterPage(doc) {
      const els = this.queryAll(doc, [this.selectPagination, "ul.pagination li[onclick]"]);
      let max = 1;
      for (const li of els) {
        const onclick = li.getAttribute("onclick") || "";
        const m = onclick.substring(onclick.lastIndexOf("=") + 1).match(/\d+/);
        const n = m ? parseInt(m[0], 10) : NaN;
        if (Number.isFinite(n) && n > max)
          max = n;
      }
      return max;
    }
    parseChapters(doc, indexOffset = 0) {
      const els = this.queryAll(doc, [this.selectChapters, "div#chapters-list > a[href]", "div#chapters-list a[href]"]);
      const chapters = [];
      els.forEach((a, i) => {
        const href = a.getAttribute("href");
        if (!href)
          return;
        const relHref = this.toRelativeUrl(href);
        const nameEl = this.queryFirst(a, this.selectChapterName);
        const name = this.text(nameEl) || this.text(a);
        const dateEl = this.queryFirst(a, this.selectChapterDate);
        const dateText = this.text(dateEl);
        const last = relHref.replace(/\/+$/, "").split("/").pop() || "";
        const numStr = last.replace(/-/g, ".").replace(/[^0-9.]/g, "");
        const number = parseFloat(numStr);
        chapters.push(new MangaChapter({
          id: relHref,
          url: relHref,
          title: name,
          number: Number.isFinite(number) ? number : indexOffset + i + 1,
          volume: 0,
          branch: null,
          scanlator: null,
          uploadDate: this.parseChapterDate(dateText),
          source: this.source
        }));
      });
      return chapters;
    }
    async getDetails(manga) {
      const mangaUrl = this.toAbsoluteUrl(manga.url);
      const html = await this.httpGetFollow(mangaUrl);
      const doc = this.context.parseHTML(html);
      const maxPage = this.maxChapterPage(doc);
      let chapters = this.parseChapters(doc);
      if (maxPage > 1) {
        const base = mangaUrl.replace(/\?.*$/, "");
        for (let p = 2; p <= maxPage; p++) {
          try {
            const sep = base.includes("?") ? "&" : "?";
            const more = await this.httpGetFollow(`${base}${sep}page=${p}`);
            chapters = chapters.concat(this.parseChapters(this.context.parseHTML(more), chapters.length));
          } catch {
            break;
          }
        }
      }
      chapters.reverse();
      const author = this.findLabeledValue(doc, this.selectAuthorLabels);
      const description = this.parseDescription(doc) || (manga.description || "");
      const altTitle = this.parseAltTitle(doc);
      const title = this.parseTitle(doc) || manga.title;
      return new Manga({
        ...manga,
        title: title || manga.title,
        altTitles: altTitle ? [altTitle] : manga.altTitles || [],
        authors: author ? [author] : manga.authors || [],
        description,
        state: this.parseState(doc),
        tags: this.parseTags(doc),
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        chapters
      });
    }
    // ---- pages -----------------------------------------------------------
    async getPages(chapter) {
      const fullUrl = this.toAbsoluteUrl(chapter.url);
      const html = await this.httpGetFollow(fullUrl);
      const doc = this.context.parseHTML(html);
      const imgs = this.queryAll(doc, this.selectPages);
      const pages = [];
      for (const img of imgs) {
        const url = this.imageSrc(img);
        if (!url || url.startsWith("data:"))
          continue;
        pages.push(new MangaPage({
          id: url,
          url,
          preview: null,
          source: this.source
        }));
      }
      return pages;
    }
    // ---- date parsing (mirrors Kotlin parseChapterDate/parseRelativeDate) -
    parseChapterDate(dateText) {
      if (!dateText)
        return 0;
      const d = dateText.toLowerCase().trim();
      if (!d)
        return 0;
      if (/(ago|مضت)$/.test(d) || /^(il y a|منذ)/.test(d)) {
        return this.parseRelativeDate(d);
      }
      const t = Date.parse(dateText);
      return Number.isFinite(t) ? t : 0;
    }
    parseRelativeDate(date) {
      const m = date.match(/(\d+)/);
      const n = m ? parseInt(m[1], 10) : 0;
      if (!n)
        return 0;
      const now = Date.now();
      const any = (words) => words.some((w) => date.includes(w));
      const MIN = 6e4, HOUR = 36e5, DAY = 864e5;
      if (any(["detik", "segundo", "second", "\u062B\u0648\u0627\u0646"]))
        return now - n * 1e3;
      if (any(["menit", "dakika", "min", "minute", "minutes", "minuto", "mins", "ph\xFAt", "\u043C\u0438\u043D\u0443\u0442", "\u062F\u0642\u064A\u0642\u0629"]))
        return now - n * MIN;
      if (any(["jam", "saat", "heure", "hora", "horas", "hour", "hours", "\u0633\u0627\u0639\u0627\u062A", "\u0633\u0627\u0639\u0629"]))
        return now - n * HOUR;
      if (any(["jour", "d\xEDa", "dia", "day", "days", "hari", "g\xFCn", "\u0434\u0435\u043D\u044C"]))
        return now - n * DAY;
      if (any(["semaine", "week", "weeks", "semana", "semanas"]))
        return now - n * 7 * DAY;
      if (any(["mois", "month", "months", "\u0623\u0634\u0647\u0631"]))
        return now - n * 30 * DAY;
      if (any(["ann\xE9e", "an", "ans", "year", "years"]))
        return now - n * 365 * DAY;
      return 0;
    }
  };

  // src/uzaymanga.js
  var UzayMangaParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 24) {
      super(context, source, domain, pageSize);
      this.cdnUrl = domain === "tenshimanga.com" ? "https://cdn-t.efsaneler2.can.re" : domain === "uzaymanga.com" ? "https://cdn-u.efsaneler2.can.re" : null;
      this._cdnResolved = false;
      this.trMonths = {
        oca: 0,
        \u015Fub: 1,
        sub: 1,
        mar: 2,
        nis: 3,
        may: 4,
        haz: 5,
        tem: 6,
        a\u011Fu: 7,
        agu: 7,
        eyl: 8,
        eki: 9,
        kas: 10,
        ara: 11
      };
      this.selectDetailsTitle = ["#content h1", "h1"];
      this.selectChapterLinks = ["div.list-episode a", "a[href*='-bolum-oku']", "a[href*='/manga/']"];
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // ---- CDN / image helpers ---------------------------------------------------
    /** Resolve PUBLIC_CDN_URL from the live homepage HTML once per run. */
    async resolveCdn() {
      if (this._cdnResolved)
        return this.cdnUrl;
      this._cdnResolved = true;
      try {
        const html = await this.context.httpGet(`https://${this.domain}/`, this);
        const m = html.match(/"PUBLIC_CDN_URL"\s*:\s*"([^"]+)"/);
        if (m && m[1])
          this.cdnUrl = m[1];
      } catch {
      }
      return this.cdnUrl;
    }
    /** Build an absolute image URL: prefer the CDN, else the site domain. */
    cdnImage(path) {
      if (!path)
        return "";
      if (/^https?:\/\//i.test(path))
        return path;
      const rel = String(path).replace(/^\/+/, "");
      const base = (this.cdnUrl || `https://${this.domain}`).replace(/\/+$/, "");
      return `${base}/${rel}`;
    }
    // ---- SvelteKit __data.json decoding ---------------------------------------
    /**
     * SvelteKit serializes page data as a flat, index-referenced array (devalue).
     * Each node's `data` is `[root, ...pool]`; values are integer indices into the
     * pool, -1 means undefined. Rehydrate into a normal object graph.
     */
    decodeDevalue(flat) {
      const seen = /* @__PURE__ */ new Map();
      const resolve = (i) => {
        if (i === -1 || i == null)
          return void 0;
        if (typeof i !== "number")
          return i;
        if (seen.has(i))
          return seen.get(i);
        const v = flat[i];
        if (Array.isArray(v)) {
          const out = [];
          seen.set(i, out);
          for (const e of v)
            out.push(resolve(e));
          return out;
        }
        if (v && typeof v === "object") {
          const out = {};
          seen.set(i, out);
          for (const k of Object.keys(v))
            out[k] = resolve(v[k]);
          return out;
        }
        return v;
      };
      return resolve(0);
    }
    /** Fetch a SvelteKit __data.json and merge every data node into one object. */
    async fetchData(pathname) {
      const base = pathname.replace(/\/+$/, "");
      const url = `https://${this.domain}${base}/__data.json`;
      const raw = await this.context.httpGet(url, this);
      let json;
      try {
        json = JSON.parse(raw);
      } catch {
        return null;
      }
      if (json && json.type === "redirect" && json.location) {
        return this.fetchData(json.location);
      }
      if (!json || !Array.isArray(json.nodes))
        return null;
      const merged = {};
      for (const node of json.nodes) {
        if (node && node.type === "data" && Array.isArray(node.data)) {
          const obj = this.decodeDevalue(node.data);
          if (obj && typeof obj === "object")
            Object.assign(merged, obj);
        }
      }
      return merged;
    }
    // ---- Mapping helpers -------------------------------------------------------
    mangaUrlForSlug(slug) {
      return `/manga/${String(slug).replace(/^\/+/, "")}`;
    }
    parseStatus(status, statusText) {
      if (status === 1 || status === "1")
        return MangaState.ONGOING;
      if (status === 2 || status === "2")
        return MangaState.FINISHED;
      if (status === 3 || status === "3")
        return MangaState.ABANDONED;
      if (status === 4 || status === "4")
        return MangaState.PAUSED;
      const v = (statusText || "").toLowerCase();
      if (!v)
        return void 0;
      if (v.includes("devam ediyor"))
        return MangaState.ONGOING;
      if (v.includes("tamamland"))
        return MangaState.FINISHED;
      if (v.includes("birak") || v.includes("b\u0131rak"))
        return MangaState.ABANDONED;
      if (v.includes("ara ver"))
        return MangaState.PAUSED;
      return void 0;
    }
    parseDate(text) {
      if (!text)
        return 0;
      const m = String(text).trim().match(/([A-Za-zÇĞİÖŞÜçğıöşü]+)\s+(\d{1,2})\s*,?\s*(\d{4})/);
      if (!m)
        return 0;
      const mon = this.trMonths[m[1].toLowerCase().slice(0, 3)];
      if (mon == null)
        return 0;
      const d = new Date(Date.UTC(Number(m[3]), mon, Number(m[2])));
      return isNaN(d.getTime()) ? 0 : d.getTime();
    }
    /** Build a Manga stub from a SvelteKit series-card object {slug,name,image,point,...}. */
    cardToManga(card) {
      if (!card || !card.slug)
        return null;
      const url = this.mangaUrlForSlug(card.slug);
      return new Manga({
        id: url,
        url,
        publicUrl: this.toAbsoluteUrl(url),
        coverUrl: this.cdnImage(card.image),
        title: (card.name || card.title || "").trim(),
        rating: card.point != null ? Math.min(1, Number(card.point) / 10) : 0,
        source: this.source,
        contentRating: this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
      });
    }
    // ---- Public API ------------------------------------------------------------
    async getListPage(page, order, filter = {}) {
      await this.resolveCdn();
      const query = (filter && filter.query ? String(filter.query) : "").trim();
      const tags = filter && filter.tags || [];
      if (query) {
        if (page > 1)
          return [];
        const found = await this.searchByApi(query);
        if (found.length)
          return found;
      }
      const data = await this.fetchData("/");
      if (!data)
        return [];
      let cards = [];
      const popular = data.seriesPopular || {};
      const seen = /* @__PURE__ */ new Set();
      const collect = (arr) => {
        for (const c of arr || []) {
          if (c && c.slug && !seen.has(c.slug)) {
            seen.add(c.slug);
            cards.push(c);
          }
        }
      };
      if (order === SortOrder.POPULARITY || order === SortOrder.RATING) {
        collect(popular.monthly);
        collect(popular.weekly);
        collect(popular.daily);
      } else if (order === SortOrder.NEWEST) {
        collect(data.newSeries);
        collect(popular.monthly);
      } else if (order === SortOrder.UPDATED) {
        collect(data.lastEpisodes && data.lastEpisodes.data);
        collect(data.newSeries);
      }
      collect(popular.daily);
      collect(popular.weekly);
      collect(popular.monthly);
      collect(data.newSeries);
      collect(data.slider);
      collect(data.lastEpisodes && data.lastEpisodes.data);
      let result = cards.map((c) => this.cardToManga(c)).filter(Boolean);
      if (query) {
        const q = query.toLowerCase();
        result = result.filter((m) => m.title.toLowerCase().includes(q));
      }
      if (tags.length) {
        const wanted = new Set(tags.map((t) => String(t.key || t).toLowerCase()));
        const tagged = cards.filter(
          (c) => Array.isArray(c.tags) && c.tags.some((t) => wanted.has(String(t).toLowerCase()))
        );
        if (tagged.length)
          result = tagged.map((c) => this.cardToManga(c)).filter(Boolean);
      }
      return page > 1 ? [] : result;
    }
    /** Navbar autocomplete API: returns [{id,name,image,...}]. */
    async searchByApi(query) {
      const url = `https://${this.domain}/api/series/search/navbar?search=${encodeURIComponent(query)}`;
      let raw;
      try {
        raw = await this.context.httpGet(url, this);
      } catch {
        return [];
      }
      const trimmed = (raw || "").trim();
      if (!trimmed || trimmed[0] !== "[")
        return [];
      let arr;
      try {
        arr = JSON.parse(trimmed);
      } catch {
        return [];
      }
      return arr.map((item) => {
        if (!item)
          return null;
        const slug = item.slug || item.id;
        if (!slug)
          return null;
        return this.cardToManga({
          slug,
          name: item.name,
          image: item.image,
          point: item.point
        });
      }).filter(Boolean);
    }
    async getDetails(manga) {
      await this.resolveCdn();
      const rel = this.toRelativeUrl(manga.url);
      const data = await this.fetchData(rel);
      const series = data && data.series;
      if (series) {
        const slug = series.slug || rel.replace(/^\/manga\//, "");
        const url = this.mangaUrlForSlug(slug);
        const cats = series.resolvedCategories || [];
        const episodes = Array.isArray(series.SeriesEpisode) ? series.SeriesEpisode.slice() : [];
        episodes.sort((a, b) => (Number(a.order) || 0) - (Number(b.order) || 0));
        const chapters = episodes.map((ep, i) => {
          const cUrl = `${url}/${String(ep.slug).replace(/^\/+/, "")}`;
          const num = Number(ep.order);
          const title = ep.name && String(ep.name).trim() ? String(ep.name).trim() : `B\xF6l\xFCm ${ep.order}`;
          return new MangaChapter({
            id: cUrl,
            url: cUrl,
            title,
            number: isFinite(num) ? num : i + 1,
            source: this.source
          });
        }).filter((c) => c.url && String(c.url).includes("-bolum-oku"));
        return new Manga({
          ...manga,
          id: url,
          url,
          publicUrl: this.toAbsoluteUrl(url),
          title: (series.name || manga.title || "").trim(),
          altTitles: [series.nameRomaji, series.nameNative].filter((t) => t && t !== series.name),
          coverUrl: this.cdnImage(series.image) || manga.coverUrl,
          largeCoverUrl: this.cdnImage(series.image) || manga.largeCoverUrl || manga.coverUrl,
          description: series.description || manga.description || "",
          rating: series.point != null ? Math.min(1, Number(series.point) / 10) : manga.rating || 0,
          tags: cats.map((c) => ({ key: c.slug || c.id, title: c.title || c.name })),
          state: this.parseStatus(series.publicStatus != null ? series.publicStatus : series.status),
          contentRating: this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
          source: this.source,
          chapters
        });
      }
      return this.getDetailsFromHtml(manga, rel);
    }
    async getDetailsFromHtml(manga, rel) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(rel), this);
      const doc = this.context.parseHTML(html);
      const content = doc.getElementById("content") || doc;
      const titleEl = this.queryAll(content, this.selectDetailsTitle)[0] || this.queryAll(doc, this.selectDetailsTitle)[0];
      const title = titleEl ? titleEl.textContent.trim() : manga.title;
      const links = this.queryAll(doc, this.selectChapterLinks);
      const seen = /* @__PURE__ */ new Set();
      const raw = [];
      for (const a of links) {
        const href = a.getAttribute("href") || "";
        if (!href.includes("-bolum-oku"))
          continue;
        const r = this.toRelativeUrl(href).replace(/\/$/, "");
        if (seen.has(r))
          continue;
        seen.add(r);
        const numM = r.match(/\/(\d+(?:[.-]\d+)?)-bolum-oku/);
        const num = numM ? Number(numM[1].replace("-", ".")) : null;
        const t = (a.querySelector("h3")?.textContent || a.textContent || "").trim();
        raw.push({ url: r, num, title: t });
      }
      raw.sort((x, y) => (x.num || 0) - (y.num || 0));
      const chapters = raw.map((c, i) => new MangaChapter({
        id: c.url,
        url: c.url,
        title: c.title || `B\xF6l\xFCm ${c.num != null ? c.num : i + 1}`,
        number: c.num != null ? c.num : i + 1,
        source: this.source
      }));
      const cover = this.imageSrc(content.querySelector("img")) || manga.coverUrl;
      const desc = content.querySelector("div.grid h2 + p")?.textContent?.trim() || manga.description || "";
      return new Manga({
        ...manga,
        title,
        coverUrl: cover,
        largeCoverUrl: cover,
        description: desc,
        contentRating: this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        source: this.source,
        chapters
      });
    }
    async getPages(chapter) {
      await this.resolveCdn();
      const rel = this.toRelativeUrl(chapter.url);
      const data = await this.fetchData(rel);
      const episode = data && data.episode;
      let images = episode && Array.isArray(episode.images) ? episode.images : null;
      if (!images || !images.length) {
        images = await this.getPagesFromRaw(rel);
      }
      if (!images || !images.length)
        return [];
      return images.map((p, i) => {
        const url = this.cdnImage(p);
        return new MangaPage({
          id: url || String(i),
          url,
          source: this.source
        });
      }).filter((p) => p.url);
    }
    /** Last-ditch page extraction: pull "/_manga/<id>/<chap>/<n>.<ext>" paths from raw HTML/JSON. */
    async getPagesFromRaw(rel) {
      let raw = "";
      try {
        raw = await this.context.httpGet(`https://${this.domain}${rel.replace(/\/+$/, "")}/__data.json`, this);
      } catch {
        try {
          raw = await this.context.httpGet(this.toAbsoluteUrl(rel), this);
        } catch {
          return [];
        }
      }
      const out = [];
      const seen = /* @__PURE__ */ new Set();
      const re = /(?:\\?"path\\?"\s*:\s*\\?"|")((?:https?:\/\/[^"\\]+|\/?_manga\/[^"\\]+)\.(?:avif|webp|jpe?g|png))/gi;
      let m;
      while ((m = re.exec(raw)) !== null) {
        const path = m[1];
        if (seen.has(path))
          continue;
        seen.add(path);
        out.push(path);
      }
      return out;
    }
  };

  // src/comicaso.js
  var ComicasoParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 16) {
      super(context, source, domain, pageSize);
      this.sourceLocale = "id";
      this.indexPath = "/wp-content/static/manga/index.json";
      this.detailPathPrefix = "/wp-content/static/manga/";
      this.detailPathSuffix = ".json";
      this.mangaUrlPrefix = "/komik/";
      this.pageImageSelectors = [
        "img.mjv2-page-image",
        "#readerarea img",
        "div.reading-content img",
        ".read-container img",
        "img.wp-manga-chapter-img"
      ];
      this.statusOngoing = "on-going";
      this.statusFinished = "end";
      this._mangaIndexCache = null;
    }
    // --- helpers (madara.js conventions) -----------------------------------
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    _baseUrl() {
      return this.domain.startsWith("http") ? this.domain : `https://${this.domain}`;
    }
    _toTitleCase(s) {
      return String(s || "").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
    }
    _stateFromStatus(status) {
      switch (status) {
        case this.statusOngoing:
          return MangaState.ONGOING;
        case this.statusFinished:
          return MangaState.FINISHED;
        default:
          return void 0;
      }
    }
    _contentRating() {
      return this.source && this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE;
    }
    _genreTags(genres) {
      if (!Array.isArray(genres))
        return [];
      return genres.filter(Boolean).map((g) => ({
        key: String(g).toLowerCase(),
        title: this._toTitleCase(g),
        source: this.source
      }));
    }
    _extractChapterNumber(title) {
      const m = String(title || "").match(/[\d]+(?:[.,]\d+)?/);
      if (!m)
        return 0;
      return parseFloat(m[0].replace(",", ".")) || 0;
    }
    // --- index.json (catalogue) caching ------------------------------------
    async _getMangaIndex() {
      if (this._mangaIndexCache)
        return this._mangaIndexCache;
      const url = `${this._baseUrl()}${this.indexPath}`;
      const text = await this.context.httpGet(url, this);
      let arr;
      try {
        arr = JSON.parse(text);
      } catch {
        arr = [];
      }
      this._mangaIndexCache = Array.isArray(arr) ? arr : [];
      return this._mangaIndexCache;
    }
    _dtoToManga(jo) {
      const slug = jo.slug;
      const relUrl = `${this.mangaUrlPrefix}${slug}/`;
      return new Manga({
        id: relUrl,
        url: relUrl,
        publicUrl: `${this._baseUrl()}${relUrl}`,
        coverUrl: jo.thumbnail || "",
        largeCoverUrl: jo.thumbnail || "",
        title: jo.title || "",
        altTitles: jo.alternative ? [jo.alternative] : [],
        tags: this._genreTags(jo.genres),
        authors: [jo.author, jo.artist].filter(Boolean).filter((v, i, a) => a.indexOf(v) === i),
        state: this._stateFromStatus(jo.status),
        source: this.source,
        contentRating: this._contentRating()
      });
    }
    // --- list / search / filter / sort -------------------------------------
    async getListPage(page, order, filter = {}) {
      let list = await this._getMangaIndex();
      if (filter.query) {
        const q = String(filter.query).toLowerCase();
        list = list.filter((it) => String(it.title || "").toLowerCase().includes(q));
      }
      const tag = filter.tags && filter.tags.length ? filter.tags[0] : null;
      if (tag) {
        const tagKey = String(tag.key || tag).toLowerCase();
        list = list.filter((dto) => Array.isArray(dto.genres) && dto.genres.some((g) => String(g).toLowerCase() === tagKey));
      }
      const state = filter.states && filter.states.length ? filter.states[0] : null;
      if (state) {
        let statusStr = null;
        if (state === MangaState.ONGOING)
          statusStr = this.statusOngoing;
        else if (state === MangaState.FINISHED)
          statusStr = this.statusFinished;
        if (statusStr)
          list = list.filter((it) => it.status === statusStr);
      }
      const type = filter.types && filter.types.length ? filter.types[0] : null;
      if (type) {
        const typeStr = String(type).toLowerCase();
        if (typeStr === "manga" || typeStr === "manhwa" || typeStr === "manhua") {
          list = list.filter((it) => String(it.type || "").toLowerCase() === typeStr);
        }
      }
      if (order === SortOrder.UPDATED) {
        list = [...list].sort((a, b) => (b.updated_at || b.manga_date || 0) - (a.updated_at || a.manga_date || 0));
      } else if (order === SortOrder.ALPHABETICAL) {
        list = [...list].sort((a, b) => String(a.title || "").toLowerCase().localeCompare(String(b.title || "").toLowerCase()));
      }
      const start = (page - 1) * this.pageSize;
      if (start >= list.length)
        return [];
      const end = Math.min(start + this.pageSize, list.length);
      return list.slice(start, end).map((dto) => this._dtoToManga(dto));
    }
    // --- details (per-manga JSON document) ---------------------------------
    async getDetails(manga) {
      const slug = String(manga.url || "").replace(this.mangaUrlPrefix, "").replace(/\/$/, "");
      const url = `${this._baseUrl()}${this.detailPathPrefix}${slug}${this.detailPathSuffix}`;
      const text = await this.context.httpGet(url, this);
      const json = JSON.parse(text);
      const synopsis = (json.synopsis || "").trim();
      const alternative = (json.alternative || "").trim();
      let description = "";
      if (synopsis)
        description += synopsis;
      if (alternative) {
        if (description)
          description += "\n\n";
        description += `Alternative: ${alternative}`;
      }
      description = description.trim();
      const tags = this._genreTags(json.genres);
      const authors = [json.author, json.artist].filter(Boolean).filter((v, i, a) => a.indexOf(v) === i);
      const chArr = Array.isArray(json.chapters) ? json.chapters : [];
      const chapters = chArr.map((ch) => {
        const chSlug = ch.slug;
        const chTitle = ch.title || "";
        const relUrl = `${this.mangaUrlPrefix}${slug}/${chSlug}/`;
        const date = ch.date ? Number(ch.date) * 1e3 : 0;
        return new MangaChapter({
          id: relUrl,
          url: relUrl,
          title: chTitle,
          number: this._extractChapterNumber(chTitle),
          volume: 0,
          uploadDate: date,
          scanlator: null,
          branch: null,
          source: this.source
        });
      });
      chapters.reverse();
      return new Manga({
        ...manga,
        title: json.title || manga.title,
        description: description || manga.description,
        coverUrl: json.thumbnail || manga.coverUrl,
        largeCoverUrl: json.thumbnail || manga.largeCoverUrl || manga.coverUrl,
        altTitles: alternative ? [alternative] : manga.altTitles,
        tags,
        state: this._stateFromStatus(json.status) ?? manga.state,
        authors,
        source: this.source,
        contentRating: this._contentRating(),
        chapters
      });
    }
    // --- pages (chapter reader HTML) ---------------------------------------
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      if (html.includes("Just a moment") || html.includes("challenges.cloudflare.com")) {
        return [];
      }
      const doc = this.context.parseHTML(html);
      const images = this.queryAll(doc, this.pageImageSelectors);
      return images.map((img) => {
        const imageUrl = this.imageSrc(img);
        return new MangaPage({
          id: imageUrl,
          url: imageUrl,
          source: this.source
        });
      }).filter((p) => p.url);
    }
  };

  // src/mangotheme.js
  var MangoThemeParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 24) {
      super(context, source, domain, pageSize);
      this.cdnUrl = `https://cdn.${domain}`;
      this.apiBaseUrl = `https://api.${domain}/api`;
      this.encryptionKey = "mangotoons_encryption_key_2025";
      this.apiToken = "bunker_api_token_secreto_2025";
      this.webMangaPathSegment = "manga";
      this.latestPageSize = 24;
      this.searchPageSize = 20;
      this.decryptSalt = "salt";
      this.statusIdsByState = {
        ONGOING: ["1", "6"],
        FINISHED: ["3"],
        PAUSED: ["2", "5"],
        ABANDONED: ["4"]
      };
      this.adultFormatIds = ["23"];
    }
    // ---- low-level JSON / crypto helpers ---------------------------------
    apiUrl(path) {
      const base = (this.apiBaseUrl || `https://${this.domain}/api`).replace(/\/+$/, "");
      return `${base}/${String(path).replace(/^\/+/, "")}`;
    }
    apiHeaders() {
      const headers = {
        "Referer": `https://${this.domain}/`,
        "Accept-Language": "pt-BR, pt;q=0.9, en-US;q=0.8, en;q=0.7",
        // Ask the backend to skip encryption when it honours this header;
        // we can still decrypt if it does not.
        "X-Noencryptionbritta": "1"
      };
      if (this.apiToken) {
        headers["X-API-Token"] = this.apiToken;
        headers["Authorization"] = `Bearer ${this.apiToken}`;
      }
      return headers;
    }
    hexToBytes(hex) {
      const clean = String(hex).trim();
      const out = new Uint8Array(clean.length >> 1);
      for (let i = 0; i < out.length; i++) {
        out[i] = parseInt(clean.substr(i * 2, 2), 16);
      }
      return out;
    }
    async decryptPayload(payload) {
      const trimmed = String(payload || "").trim();
      if (!trimmed)
        return "";
      if (trimmed.startsWith("{") || trimmed.startsWith("["))
        return trimmed;
      const sep = trimmed.indexOf(":");
      if (sep <= 0)
        return trimmed;
      const ivHex = trimmed.slice(0, sep);
      const cipherHex = trimmed.slice(sep + 1);
      if (!/^[0-9a-fA-F]+$/.test(ivHex) || !/^[0-9a-fA-F]+$/.test(cipherHex)) {
        return trimmed;
      }
      const subtle = globalThis.crypto && globalThis.crypto.subtle || null;
      if (!subtle)
        throw new Error("Web Crypto (crypto.subtle) unavailable for MangoTheme decrypt");
      const keyMaterial = await subtle.digest(
        "SHA-256",
        new TextEncoder().encode(this.encryptionKey + this.decryptSalt)
      );
      const key = await subtle.importKey("raw", keyMaterial, { name: "AES-CBC" }, false, ["decrypt"]);
      const plain = await subtle.decrypt(
        { name: "AES-CBC", iv: this.hexToBytes(ivHex) },
        key,
        this.hexToBytes(cipherHex)
      );
      return new TextDecoder().decode(plain);
    }
    async requestJson(url) {
      const raw = await this.context.httpGet(url, this, this.apiHeaders());
      const text = await this.decryptPayload(raw);
      try {
        return JSON.parse(text);
      } catch {
        return {};
      }
    }
    // ---- url helpers (mirror the Kotlin internal-url scheme) -------------
    toAbsoluteCdnUrl(value) {
      const v = String(value || "");
      if (!v)
        return "";
      if (v.startsWith("http://") || v.startsWith("https://"))
        return v;
      return `${(this.cdnUrl || "").replace(/\/+$/, "")}/${v.replace(/^\/+/, "")}`;
    }
    buildInternalMangaUrl(mangaId, slug) {
      let url = `/obra/${mangaId}`;
      if (slug)
        url += `?slug=${slug}`;
      return url;
    }
    buildInternalChapterUrl(mangaId, chapterNumber, slug) {
      let url = `/obra/${mangaId}/capitulo/${chapterNumber}`;
      if (slug)
        url += `?slug=${slug}`;
      return url;
    }
    extractMangaId(url) {
      const after = String(url || "").split("/obra/")[1] || "";
      return after.split("/")[0].split("?")[0];
    }
    extractChapterNumber(url) {
      const last = String(url || "").split("/").pop() || "";
      return last.split("?")[0];
    }
    formatChapterNumber(numberRaw) {
      const f = parseFloat(numberRaw);
      if (Number.isNaN(f))
        return String(numberRaw);
      let s = String(f);
      return s;
    }
    // ---- parsing -----------------------------------------------------------
    getString(obj, ...keys) {
      for (const k of keys) {
        const v = obj ? obj[k] : void 0;
        if (v !== void 0 && v !== null && String(v).length)
          return String(v);
      }
      return null;
    }
    parseTags(arr) {
      if (!Array.isArray(arr))
        return [];
      const out = [];
      for (const tag of arr) {
        const id = parseInt(tag && tag.id, 10);
        if (!(id > 0))
          continue;
        const title = this.getString(tag, "nome", "name");
        if (!title)
          continue;
        out.push({ key: String(id), title });
      }
      return out;
    }
    parseState(json) {
      const statusId = json && json.status_id != null ? String(json.status_id) : null;
      if (statusId != null) {
        for (const [state, ids] of Object.entries(this.statusIdsByState)) {
          if (ids.includes(statusId))
            return state;
        }
      }
      const name = (this.getString(json, "status_nome") || "").trim().toLowerCase();
      switch (name) {
        case "ativo":
        case "em andamento":
          return MangaState.ONGOING;
        case "concluido":
        case "conclu\xEDdo":
          return MangaState.FINISHED;
        case "hiato":
        case "pausado":
          return MangaState.PAUSED;
        case "cancelado":
          return MangaState.ABANDONED;
        default:
          return void 0;
      }
    }
    parseManga(json) {
      const id = parseInt(json && json.id, 10);
      if (!(id > 0))
        return null;
      const title = this.getString(json, "nome", "title");
      if (!title)
        return null;
      const slug = this.getString(json, "slug");
      const formatId = json && json.formato_id != null ? String(json.formato_id) : null;
      const cover = this.getString(json, "imagem", "coverImage");
      const relUrl = this.buildInternalMangaUrl(String(id), slug);
      return new Manga({
        id: relUrl,
        url: relUrl,
        publicUrl: `https://${this.domain}/${this.webMangaPathSegment}/${slug || id}`,
        coverUrl: cover ? this.toAbsoluteCdnUrl(cover) : "",
        title,
        description: this.getString(json, "descricao") || "",
        tags: this.parseTags(json && json.tags),
        state: this.parseState(json),
        authors: [],
        source: this.source,
        contentRating: formatId && this.adultFormatIds.includes(formatId) || this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
      });
    }
    parseTopManga(json) {
      const id = parseInt(json && json.id, 10);
      if (!(id > 0))
        return null;
      const title = this.getString(json, "title", "nome");
      if (!title)
        return null;
      const cover = this.getString(json, "coverImage", "imagem");
      const relUrl = this.buildInternalMangaUrl(String(id), null);
      return new Manga({
        id: relUrl,
        url: relUrl,
        publicUrl: `https://${this.domain}/${this.webMangaPathSegment}/${id}`,
        coverUrl: cover ? this.toAbsoluteCdnUrl(cover) : "",
        title,
        source: this.source,
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
      });
    }
    parseChapters(json, slug) {
      const mangaId = parseInt(json && json.id, 10);
      if (!(mangaId > 0))
        return [];
      const arr = json && json.capitulos || [];
      if (!Array.isArray(arr))
        return [];
      const chapters = [];
      for (const ch of arr) {
        const numberRaw = this.getString(ch, "numero");
        if (numberRaw == null)
          continue;
        const numberFormatted = this.formatChapterNumber(numberRaw);
        const rawName = this.getString(ch, "nome");
        const title = rawName && rawName.toLowerCase() !== `cap. ${numberFormatted}`.toLowerCase() ? rawName : null;
        chapters.push(new MangaChapter({
          id: `${mangaId}_${numberFormatted}`,
          url: this.buildInternalChapterUrl(String(mangaId), numberFormatted, slug),
          title,
          number: parseFloat(numberRaw) || 0,
          volume: 0,
          scanlator: null,
          branch: null,
          uploadDate: this.parseApiDate(this.getString(ch, "criado_em", "atualizado_em")),
          source: this.source
        }));
      }
      chapters.sort((a, b) => a.number - b.number);
      return chapters;
    }
    parseApiDate(dateString) {
      if (!dateString)
        return 0;
      const t = Date.parse(dateString);
      return Number.isNaN(t) ? 0 : t;
    }
    // ---- public API --------------------------------------------------------
    async getListPage(page, order, filter) {
      filter = filter || {};
      const hasQuery = filter.query && String(filter.query).trim().length > 0;
      const hasTags = filter.tags && filter.tags.length;
      const hasStates = filter.states && filter.states.length;
      const isEmpty = !hasQuery && !hasTags && !hasStates;
      if (isEmpty && order === SortOrder.POPULARITY) {
        return this.getPopularPage(page);
      }
      if (isEmpty) {
        return this.getLatestPage(page);
      }
      return this.search(page, filter);
    }
    async getPopularPage(page) {
      if (page > 1)
        return [];
      const res = await this.requestJson(this.apiUrl("obras/top10/views?periodo=total"));
      const arr = res && res.obras || [];
      return Array.isArray(arr) ? arr.map((j) => this.parseTopManga(j)).filter(Boolean) : [];
    }
    async getLatestPage(page) {
      const res = await this.requestJson(
        this.apiUrl(`capitulos/recentes?pagina=${page}&limite=${this.latestPageSize}`)
      );
      const arr = res && res.obras || [];
      if (!Array.isArray(arr))
        return [];
      const seen = /* @__PURE__ */ new Set();
      const out = [];
      for (const j of arr) {
        const m = this.parseManga(j);
        if (m && !seen.has(m.url)) {
          seen.add(m.url);
          out.push(m);
        }
      }
      return out;
    }
    async search(page, filter) {
      const limit = filter.query && String(filter.query).trim() ? this.searchPageSize : this.latestPageSize;
      let url = this.apiUrl(`obras?pagina=${page}`) + `&limite=${limit}`;
      const q = filter.query && String(filter.query).trim();
      if (q)
        url += `&busca=${encodeURIComponent(q)}`;
      if (filter.tags && filter.tags.length) {
        url += `&tag_ids=${filter.tags.map((t) => encodeURIComponent(t.key)).join(",")}`;
      }
      if (filter.states && filter.states.length) {
        const ids = this.statusIdsByState[filter.states[0]];
        if (ids && ids.length)
          url += `&status_id=${ids.join(",")}`;
      }
      const res = await this.requestJson(url);
      const arr = res && res.obras || [];
      return Array.isArray(arr) ? arr.map((j) => this.parseManga(j)).filter(Boolean) : [];
    }
    async getDetails(manga) {
      const mangaId = this.extractMangaId(manga.url);
      const res = await this.requestJson(this.apiUrl(`obras/${mangaId}`));
      const item = res && (res.obra || res.data || res.dados) || res || {};
      const parsed = this.parseManga(item) || manga;
      const slug = this.getString(item, "slug") || "";
      const chapters = this.parseChapters(item, slug);
      return new Manga({
        ...manga,
        title: parsed.title && parsed.title.trim() || manga.title,
        url: parsed.url || manga.url,
        publicUrl: parsed.publicUrl || manga.publicUrl,
        coverUrl: parsed.coverUrl || manga.coverUrl,
        largeCoverUrl: parsed.coverUrl || manga.largeCoverUrl || manga.coverUrl,
        description: parsed.description || manga.description,
        tags: parsed.tags && parsed.tags.length ? parsed.tags : manga.tags,
        authors: parsed.authors && parsed.authors.length ? parsed.authors : manga.authors,
        state: parsed.state || manga.state,
        contentRating: parsed.contentRating || manga.contentRating,
        source: this.source,
        chapters
      });
    }
    async getPages(chapter) {
      const mangaId = this.extractMangaId(chapter.url);
      const number = this.extractChapterNumber(chapter.url);
      const res = await this.requestJson(
        this.apiUrl(`obras/${mangaId}/capitulos/${encodeURIComponent(number)}`)
      );
      const item = res && (res.capitulo || res.data || res.dados) || res || {};
      const paginas = item && item.paginas;
      if (!Array.isArray(paginas))
        return [];
      const pages = [];
      for (const p of paginas) {
        const raw = this.getString(p, "cdn_id", "imagem", "image", "src", "link", "path", "arquivo");
        if (!raw)
          continue;
        const imageUrl = this.toAbsoluteCdnUrl(raw);
        pages.push(new MangaPage({
          id: imageUrl,
          url: imageUrl,
          source: this.source
        }));
      }
      pages.sort((a, b) => {
        const na = parseInt((String(a.url).split("/pagina_")[1] || "").split(".")[0], 10);
        const nb = parseInt((String(b.url).split("/pagina_")[1] || "").split(".")[0], 10);
        return (Number.isNaN(na) ? Number.MAX_SAFE_INTEGER : na) - (Number.isNaN(nb) ? Number.MAX_SAFE_INTEGER : nb);
      });
      return pages;
    }
  };

  // src/zmanga.js
  var ZMangaParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 16) {
      super(context, source, domain, pageSize);
      this.listUrl = "advanced-search/";
      this.datePattern = "MMMM d, yyyy";
      this.selectDesc = "div.series-synops";
      this.selectState = "span.status";
      this.selectAlt = "div.series-infolist li:contains(Alt) span";
      this.selectAut = "div.series-infolist li:contains(Author) span";
      this.selectTag = "div.series-genres a";
      this.selectDate = "span.date";
      this.selectChapter = "ul.series-chapterlist li";
      this.selectChapterTitle = ".flexch-infoz span:not(.date)";
      this.selectPage = "div.reader-area img";
      this.ongoing = /* @__PURE__ */ new Set(["on going", "ongoing"]);
      this.finished = /* @__PURE__ */ new Set(["completed"]);
    }
    // madara.js-style multi-selector fallback helper
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    queryFirst(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const el = doc.querySelector(selector);
          if (el)
            return el;
        } catch {
        }
      }
      return null;
    }
    // madara.js-style image src extraction with lazy-load fallbacks
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // ---- list ----
    // https://komikindo.info/advanced-search/?title=the&yearx=2020&status=ongoing&type=Manga&order=update
    async getListPage(page, order, filter = {}) {
      let url = `https://${this.domain}/${this.listUrl}`;
      if (page > 1)
        url += `page/${page}/`;
      url += "?order=";
      switch (order) {
        case SortOrder.POPULARITY:
          url += "popular";
          break;
        case SortOrder.UPDATED:
          url += "update";
          break;
        case SortOrder.ALPHABETICAL:
          url += "title";
          break;
        case SortOrder.ALPHABETICAL_DESC:
          url += "titlereverse";
          break;
        case SortOrder.NEWEST:
          url += "latest";
          break;
        case SortOrder.RATING:
          url += "rating";
          break;
        default:
          url += "update";
          break;
      }
      if (filter.query)
        url += `&title=${encodeURIComponent(filter.query)}`;
      if (filter.year)
        url += `&yearx=${filter.year}`;
      if (filter.tags && filter.tags.length) {
        for (const t of filter.tags) {
          const key = t && (t.key || t);
          if (key)
            url += `&${encodeURIComponent("genre[]")}=${key}`;
        }
      }
      if (filter.states && filter.states.length) {
        const st = filter.states[0];
        if (st === MangaState.ONGOING)
          url += "&status=ongoing";
        else if (st === MangaState.FINISHED)
          url += "&status=completed";
      }
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, [
        "div.flexbox2-item",
        ".flexbox2-content",
        ".flexbox-item"
      ]);
      const list = [];
      for (const div of elements) {
        const a = div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const titleEl = this.queryFirst(div, [
          "div.flexbox2-title span:not(.studio)",
          "div.flexbox2-title span.title",
          "div.flexbox2-title",
          ".title"
        ]);
        const img = div.querySelector("img");
        const scoreEl = div.querySelector("div.info div.score");
        let rating = 0;
        if (scoreEl) {
          const m = (scoreEl.textContent || "").match(/[\d.]+/);
          if (m) {
            const v = parseFloat(m[0]);
            if (!isNaN(v))
              rating = v / 10;
          }
        }
        list.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title: titleEl ? titleEl.textContent.trim() : (a.getAttribute("title") || a.textContent || "").trim(),
          rating,
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return list;
    }
    // ---- details ----
    // Manual replacement for jQuery `:contains(X)` selectors used in the Kotlin source.
    findInfoValue(doc, label) {
      const items = this.queryAll(doc, [
        "div.series-infolist li",
        ".series-infolist li",
        ".infolist li"
      ]);
      const lower = label.toLowerCase();
      for (const li of items) {
        const txt = (li.textContent || "").toLowerCase();
        if (txt.includes(lower)) {
          const span = li.querySelector("span");
          const val = span ? span.textContent.trim() : "";
          if (val)
            return val;
        }
      }
      return null;
    }
    async getDetails(manga) {
      const fullUrl = this.toAbsoluteUrl(manga.url);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      const descEl = this.queryFirst(doc, [this.selectDesc, "div.series-synops", ".series-synops", ".entry-content"]);
      const description = descEl ? descEl.innerHTML : "";
      const stateEl = this.queryFirst(doc, [this.selectState, "span.status", ".status"]);
      let state;
      if (stateEl) {
        const t = (stateEl.textContent || "").trim().toLowerCase();
        if (this.ongoing.has(t))
          state = MangaState.ONGOING;
        else if (this.finished.has(t))
          state = MangaState.FINISHED;
      }
      const alt = this.findInfoValue(doc, "Alt");
      const author = this.findInfoValue(doc, "Author");
      const tagEls = this.queryAll(doc, [this.selectTag, "div.series-genres a", ".series-genres a"]);
      const tags = tagEls.map((a) => {
        const href = (a.getAttribute("href") || "").replace(/\/$/, "");
        const key = href.split("/").filter(Boolean).pop() || a.textContent.trim();
        return { key, title: (a.textContent || "").trim().replace(/,/g, ""), source: this.source };
      }).filter((t) => t.key);
      const chapters = this.getChapters(doc);
      const contentRating = doc.getElementById("adt-warning") ? ContentRating.ADULT : this.source.isNsfw ? ContentRating.ADULT : manga.contentRating || ContentRating.SAFE;
      return new Manga({
        ...manga,
        description,
        altTitles: alt ? [alt] : manga.altTitles || [],
        authors: author ? [author] : manga.authors || [],
        tags: tags.length ? tags : manga.tags || [],
        state,
        contentRating,
        source: this.source,
        chapters
      });
    }
    // Returns chapters oldest-first (Kotlin maps with reversed = true).
    getChapters(doc) {
      const elements = this.queryAll(doc, [
        this.selectChapter,
        "ul.series-chapterlist li",
        ".series-chapterlist li",
        "#chapterlist li",
        ".chapter-list li"
      ]).reverse();
      return elements.map((li, i) => {
        const a = li.querySelector("a");
        if (!a)
          return null;
        const href = a.getAttribute("href");
        if (!href)
          return null;
        const relHref = this.toRelativeUrl(href);
        let title = "";
        const titleEl = this.queryFirst(li, [this.selectChapterTitle, ".flexch-infoz span", "span"]);
        if (titleEl) {
          const dateChild = titleEl.querySelector(".date");
          title = (titleEl.textContent || "").trim();
          if (dateChild) {
            title = title.replace((dateChild.textContent || "").trim(), "").trim();
          }
        }
        if (!title) {
          title = (a.getAttribute("title") || a.textContent || "").trim();
        }
        const dateEl = this.queryFirst(li, [this.selectDate, "span.date", ".date"]);
        const uploadDate = dateEl ? this.parseChapterDate((dateEl.textContent || "").trim()) : 0;
        return new MangaChapter({
          id: relHref,
          url: relHref,
          title,
          number: i + 1,
          volume: 0,
          uploadDate,
          source: this.source
        });
      }).filter((c) => c && c.url && !c.url.includes("#"));
    }
    // ---- pages ----
    async getPages(chapter) {
      const fullUrl = this.toAbsoluteUrl(chapter.url);
      const html = await this.context.httpGet(fullUrl, this);
      const doc = this.context.parseHTML(html);
      const imgs = this.queryAll(doc, [
        this.selectPage,
        "div.reader-area img",
        ".reader-area img",
        "#readerarea img",
        ".main-reading-area img"
      ]);
      return imgs.map((img) => {
        const url = this.imageSrc(img);
        return new MangaPage({
          id: url,
          url,
          source: this.source
        });
      }).filter((p) => p.url && !p.url.startsWith("data:"));
    }
    // ---- date parsing (port of parseChapterDate / parseRelativeDate) ----
    parseChapterDate(date) {
      if (!date)
        return 0;
      const d = date.toLowerCase();
      if (/( ago| h| d)$/.test(d)) {
        return this.parseRelativeDate(d);
      }
      if (d.startsWith("today")) {
        const now = /* @__PURE__ */ new Date();
        now.setHours(0, 0, 0, 0);
        return now.getTime();
      }
      const cleaned = date.replace(/(\d+)(st|nd|rd|th)/gi, "$1");
      const ts = Date.parse(cleaned);
      return isNaN(ts) ? 0 : ts;
    }
    parseRelativeDate(date) {
      const m = date.match(/(\d+)/);
      if (!m)
        return 0;
      const n = parseInt(m[1], 10);
      const now = /* @__PURE__ */ new Date();
      if (/second/.test(date))
        now.setSeconds(now.getSeconds() - n);
      else if (/\bmin|minute/.test(date))
        now.setMinutes(now.getMinutes() - n);
      else if (/hour|\bh\b/.test(date))
        now.setHours(now.getHours() - n);
      else if (/day|\bd\b/.test(date))
        now.setDate(now.getDate() - n);
      else if (/month/.test(date))
        now.setMonth(now.getMonth() - n);
      else if (/year/.test(date))
        now.setFullYear(now.getFullYear() - n);
      else
        return 0;
      return now.getTime();
    }
  };

  // src/likemanga.js
  var LikeMangaParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 36) {
      super(context, source, domain, pageSize);
      this.referer = "https://likemanga.ink/";
      this.searchPath = "/?act=search";
      this.selectMangaList = "div.card-body div.video";
      this.selectMangaTitle = "p.title-manga";
      this.selectChapter = "li.wp-manga-chapter";
      this.selectChapterDate = ".chapter-release-date";
      this.selectNavPages = "#nav_list_chapter_id_detail a:not(.next)";
      this.selectAltTitle = ".list-info li.othername h2";
      this.selectTags = "li.kind a";
      this.selectAuthor = "li.author p";
      this.selectSummary = "#summary_shortened";
      this.ajaxChaptersPath = "/?act=ajax&code=load_list_chapter";
      this.chapterSplitToken = "wp-manga-chapter";
      this.selectReadingImg = ".reading-detail img";
      this.selectImgToken = "div.reading input#next_img_token";
      this.genresPath = "/genres/";
      this.sortValues = {
        [SortOrder.UPDATED]: "lastest-chap",
        [SortOrder.NEWEST]: "lastest-manga",
        [SortOrder.POPULARITY]: "top-manga"
      };
      this.defaultSort = "lastest-chap";
      this.stateValues = {
        [MangaState.ONGOING]: "in-process",
        [MangaState.FINISHED]: "complete",
        [MangaState.PAUSED]: "pause"
      };
    }
    // --- helpers (mirroring madara.js conventions) ---
    queryAll(doc, selectors) {
      for (const selector of selectors.filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // Site uses Referer-gated responses; thread it on every request.
    get reqHeaders() {
      return { "Referer": this.referer };
    }
    decodeBase64(b64) {
      const bin = typeof atob === "function" ? atob(b64) : Buffer.from(b64, "base64").toString("binary");
      try {
        const bytes = Uint8Array.from(bin, (c) => c.charCodeAt(0));
        return new TextDecoder("utf-8").decode(bytes);
      } catch {
        return bin;
      }
    }
    // --- list / search ---
    async getListPage(page, order, filter) {
      filter = filter || {};
      let url = `https://${this.domain}${this.searchPath}`;
      if (filter.query) {
        url += `&${encodeURIComponent("f[keyword]")}=${encodeURIComponent(filter.query)}`;
      }
      const sortby = this.sortValues[order] || this.defaultSort;
      url += `&${encodeURIComponent("f[sortby]")}=${sortby}`;
      const tags = filter.tags || [];
      if (tags.length) {
        url += `&${encodeURIComponent("f[genres]")}=${encodeURIComponent(tags[0].key)}`;
      }
      const states = filter.states || [];
      if (states.length) {
        const s = this.stateValues[states[0]] || "all";
        url += `&${encodeURIComponent("f[status]")}=${s}`;
      }
      if (page > 1) {
        url += `&pageNum=${page}`;
      }
      const html = await this.context.httpGet(url, this);
      return this.parseMangaList(html);
    }
    parseMangaList(html) {
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, [
        this.selectMangaList,
        "div.card-body div.video",
        "div.video"
      ]);
      const mangaList = [];
      for (const div of elements) {
        const a = div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const titleEl = div.querySelector(this.selectMangaTitle) || div.querySelector("p.title-manga, .title-manga, .title");
        const img = div.querySelector("img");
        mangaList.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title: (titleEl ? titleEl.textContent : a.getAttribute("title") || a.textContent || "").trim(),
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return mangaList;
    }
    // --- details ---
    mangaIdFromUrl(url) {
      const m = String(url || "").replace(/\/+$/, "").match(/-(\d+)$/);
      return m ? m[1] : null;
    }
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const mangaId = this.mangaIdFromUrl(manga.url);
      let maxPage = 1;
      for (const a of this.queryAll(doc, [this.selectNavPages, "#nav_list_chapter_id_detail a:not(.next)"])) {
        const n = parseInt((a.textContent || "").trim(), 10);
        if (!Number.isNaN(n) && n > maxPage)
          maxPage = n;
      }
      let chapters = this.parseChaptersFromDoc(doc);
      if (maxPage > 1 && mangaId) {
        for (let p = 2; p <= maxPage; p++) {
          try {
            const more = await this.loadChapters(mangaId, p);
            chapters = chapters.concat(more);
          } catch {
          }
        }
      }
      chapters.reverse();
      const altEl = doc.querySelector(this.selectAltTitle);
      const altTitle = altEl ? altEl.textContent.trim() : "";
      const author = this.lastTextOf(doc, this.selectAuthor);
      const summaryEl = doc.querySelector(this.selectSummary);
      const tags = this.queryAll(doc, [this.selectTags, "li.kind a"]).map((a) => {
        const href = a.getAttribute("href") || "";
        const key = href.replace(/\/+$/, "").split("/").pop();
        return { key, title: (a.textContent || "").trim() };
      }).filter((t) => t.key && t.title);
      return new Manga({
        ...manga,
        altTitles: altTitle ? [altTitle] : manga.altTitles || [],
        tags: tags.length ? tags : manga.tags || [],
        authors: author ? [author] : manga.authors || [],
        description: summaryEl ? summaryEl.innerHTML : manga.description || "",
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        source: this.source,
        chapters
      });
    }
    lastTextOf(doc, selector) {
      const els = this.queryAll(doc, [selector]);
      if (!els.length)
        return "";
      const t = (els[els.length - 1].textContent || "").trim();
      return t && t.toLowerCase() !== "updating" ? t : "";
    }
    chapterNumberFromUrl(url) {
      const m = String(url || "").match(/chapter-([^-/?#]+)/i);
      const n = m ? parseFloat(m[1]) : NaN;
      return Number.isNaN(n) ? 0 : n;
    }
    parseChaptersFromDoc(doc) {
      const els = this.queryAll(doc, [this.selectChapter, "li.wp-manga-chapter"]);
      const out = [];
      for (const li of els) {
        const a = li.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const dateEl = li.querySelector(this.selectChapterDate);
        const rawDate = dateEl ? dateEl.textContent.trim() : "";
        out.push(new MangaChapter({
          id: relHref,
          url: relHref,
          title: (a.textContent || "").trim(),
          number: this.chapterNumberFromUrl(relHref),
          volume: 0,
          uploadDate: this.parseChapterDate(rawDate),
          source: this.source
        }));
      }
      return out;
    }
    async loadChapters(mangaId, page) {
      const url = `https://${this.domain}${this.ajaxChaptersPath}&manga_id=${mangaId}&page_num=${page}&chap_id=0&keyword=`;
      const text = await this.context.httpGet(url, this);
      let listChap = "";
      try {
        listChap = JSON.parse(text).list_chap || "";
      } catch {
        listChap = "";
      }
      if (!listChap)
        return [];
      const parts = listChap.split(this.chapterSplitToken).slice(1);
      const out = [];
      for (const chunk of parts) {
        const href = this.between(chunk, 'href="', '"');
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        let name = this.between(chunk, '">', "</a>");
        name = (name || "").replace(/<[^>]*>/g, "").trim();
        const rawDate = this.between(chunk, "<i>", "</i>");
        out.push(new MangaChapter({
          id: relHref,
          url: relHref,
          title: name || `Chapter ${this.chapterNumberFromUrl(relHref)}`,
          number: this.chapterNumberFromUrl(relHref),
          volume: 0,
          uploadDate: this.parseChapterDate(rawDate),
          source: this.source
        }));
      }
      return out;
    }
    between(s, start, end) {
      const i = s.indexOf(start);
      if (i < 0)
        return "";
      const from = i + start.length;
      const j = s.indexOf(end, from);
      if (j < 0)
        return "";
      return s.slice(from, j);
    }
    parseChapterDate(raw) {
      if (!raw)
        return 0;
      const d = raw.toLowerCase().trim();
      if (d === "new" || d.startsWith("today")) {
        const now = /* @__PURE__ */ new Date();
        now.setHours(0, 0, 0, 0);
        return now.getTime();
      }
      const t = Date.parse(raw);
      return Number.isNaN(t) ? 0 : t;
    }
    // --- pages ---
    async getPages(chapter) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const doc = this.context.parseHTML(html);
      const tokenEl = doc.querySelector(this.selectImgToken) || doc.querySelector("input#next_img_token");
      if (tokenEl) {
        const pages = this.decodeTokenPages(doc, tokenEl);
        if (pages.length)
          return pages;
      }
      const imgs = this.queryAll(doc, [this.selectReadingImg, ".reading-detail img", "div.reading-detail img"]);
      return imgs.map((img, i) => {
        const url = this.imageSrc(img);
        return new MangaPage({ id: url || String(i), url, source: this.source });
      }).filter((p) => p.url);
    }
    decodeTokenPages(doc, tokenEl) {
      try {
        const value = tokenEl.getAttribute("value") || "";
        const middle = value.split(".")[1];
        if (!middle)
          return [];
        const jsonData = JSON.parse(this.decodeBase64(middle));
        const inner = this.decodeBase64(jsonData.data);
        const cleaned = inner.replace(/\\/g, "").replace(/\[/g, "").replace(/\]/g, "").replace(/"/g, "");
        const imgPaths = cleaned.split(",").map((s) => s.trim()).filter(Boolean);
        const firstImg = doc.querySelector(this.selectReadingImg) || doc.querySelector(".reading-detail img");
        const baseUrl = firstImg ? this.imageSrc(firstImg) : "";
        let cdn = "";
        if (baseUrl) {
          const idx = baseUrl.indexOf("manga/");
          if (idx >= 0) {
            cdn = baseUrl.slice(0, idx);
          } else {
            try {
              cdn = new URL("/", baseUrl).href;
            } catch {
              cdn = "";
            }
          }
        }
        return imgPaths.map((img, i) => {
          const url = this.concatUrl(cdn, img);
          return new MangaPage({ id: url || String(i), url, source: this.source });
        }).filter((p) => p.url);
      } catch {
        return [];
      }
    }
    concatUrl(base, path) {
      if (!path)
        return "";
      if (/^https?:\/\//i.test(path))
        return path;
      if (!base)
        return this.toAbsoluteUrl(path);
      return base.replace(/\/+$/, "") + "/" + path.replace(/^\/+/, "");
    }
  };

  // src/sinmh.js
  var SinmhParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 36) {
      super(context, source, domain, pageSize);
      this.searchUrl = "search/";
      this.listUrl = "list/";
      this.selectMangaList = ["#contList > li", "li.list-comic"];
      this.selectMangaTitle = ["p > a", "h3 > a"];
      this.selectDesc = "div#intro-all p";
      this.selectGenre = "ul.detail-list li:contains(\u6F2B\u753B\u7C7B\u578B) a";
      this.selectState = "ul.detail-list li:contains(\u6F2B\u753B\u72B6\u6001) a";
      this.genreLabel = "\u6F2B\u753B\u7C7B\u578B";
      this.stateLabel = "\u6F2B\u753B\u72B6\u6001";
      this.selectChapter = ["ul#chapter-list-1 li", "#chapter-list-1 li", "#chapter-list-4 li", ".chapter-body li"];
      this.selectTestScriptMarker = "chapterImages = ";
      this.configJsPath = "/js/config.js";
      this.ongoing = /* @__PURE__ */ new Set(["\u8FDE\u8F7D\u4E2D"]);
      this.finished = /* @__PURE__ */ new Set(["\u5DF2\u5B8C\u7ED3"]);
    }
    // --- helpers (mirrors madara.js conventions) ---
    queryAll(doc, selectors) {
      for (const selector of (selectors || []).filter(Boolean)) {
        try {
          const elements = Array.from(doc.querySelectorAll(selector));
          if (elements.length)
            return elements;
        } catch {
        }
      }
      return [];
    }
    querySelectorAny(el, selectors) {
      for (const selector of (selectors || []).filter(Boolean)) {
        try {
          const found = el.querySelector(selector);
          if (found)
            return found;
        } catch {
        }
      }
      return null;
    }
    imageSrc(img) {
      const url = img ? img.getAttribute("data-src") || img.getAttribute("data-original") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
      if (!url || url.startsWith("data:") || url.startsWith("blob:"))
        return url || "";
      return this.toAbsoluteUrl(url);
    }
    // Reproduce jsoup's ":contains(label)" for browser DOM: find the li whose text
    // includes `label`, then return its <a> elements.
    findLabeledLinks(doc, containerSelector, label) {
      const links = [];
      let lis = [];
      try {
        lis = Array.from(doc.querySelectorAll(containerSelector));
      } catch {
        lis = [];
      }
      for (const li of lis) {
        if ((li.textContent || "").includes(label)) {
          links.push(...Array.from(li.querySelectorAll("a")));
        }
      }
      return links;
    }
    tagFromHref(href) {
      return (href || "").replace(/\/+$/, "").split("/").filter(Boolean).pop() || "";
    }
    // --- list / search ---
    async getListPage(page, order, filter) {
      filter = filter || {};
      const tags = filter.tags ? Array.from(filter.tags) : [];
      const states = filter.states ? Array.from(filter.states) : [];
      let url = `https://${this.domain}/`;
      if (filter.query) {
        url += `${this.searchUrl}?keywords=${encodeURIComponent(filter.query)}&page=${page}`;
      } else {
        url += this.listUrl;
        if (tags.length) {
          url += tags[0].key;
        }
        if (states.length) {
          const st = states[0];
          if (st === MangaState.ONGOING)
            url += "-lianzai";
          else if (st === MangaState.FINISHED)
            url += "-wanjie";
        }
        if (tags.length && states.length) {
          url += "/";
        }
        switch (order) {
          case SortOrder.POPULARITY:
            url += "click/";
            break;
          case SortOrder.UPDATED:
            url += "update/";
            break;
          default:
            url += "/";
            break;
        }
        url += `${page}/`;
      }
      const html = await this.context.httpGet(url, this);
      const doc = this.context.parseHTML(html);
      const elements = this.queryAll(doc, this.selectMangaList);
      const list = [];
      for (const div of elements) {
        const a = div.querySelector("a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href)
          continue;
        const relHref = this.toRelativeUrl(href);
        const titleEl = this.querySelectorAny(div, this.selectMangaTitle);
        const img = div.querySelector("img");
        const votes = div.querySelector("span.total_votes");
        let rating = 0;
        if (votes) {
          const v = parseFloat((votes.textContent || "").trim());
          if (!isNaN(v))
            rating = v / 5;
        }
        list.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl: this.imageSrc(img),
          title: titleEl ? titleEl.textContent.trim() : (a.getAttribute("title") || a.textContent || "").trim(),
          rating,
          state: null,
          source: this.source,
          contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE
        }));
      }
      return list;
    }
    // --- details ---
    async getDetails(manga) {
      const html = await this.context.httpGet(this.toAbsoluteUrl(manga.url), this);
      const doc = this.context.parseHTML(html);
      const chapters = this.getChapters(doc);
      const descEl = this.querySelectorAny(doc, [this.selectDesc, "div#intro-all", ".intro-total", ".comic-description"]);
      const description = descEl ? descEl.innerHTML : manga.description || "";
      let state = manga.state || null;
      const stateLinks = this.findLabeledLinks(doc, "ul.detail-list li", this.stateLabel);
      const stateText = stateLinks.length ? (stateLinks[0].textContent || "").trim() : "";
      if (stateText) {
        if (this.ongoing.has(stateText))
          state = MangaState.ONGOING;
        else if (this.finished.has(stateText))
          state = MangaState.FINISHED;
      }
      const genreLinks = this.findLabeledLinks(doc, "ul.detail-list li", this.genreLabel);
      const tags = genreLinks.map((a) => {
        const key = this.tagFromHref(a.getAttribute("href"));
        return { key, title: (a.textContent || "").trim(), source: this.source };
      }).filter((t) => t.key);
      const title = doc.querySelector("h1")?.textContent?.trim() || manga.title;
      const cover = this.imageSrc(doc.querySelector(".banner_detail_form .cover img, .de-info__cover img, .comic-cover img")) || manga.coverUrl || "";
      return new Manga({
        ...manga,
        title,
        coverUrl: cover || manga.coverUrl,
        largeCoverUrl: cover || manga.largeCoverUrl || manga.coverUrl,
        description,
        state,
        tags: tags.length ? tags : manga.tags,
        contentRating: this.source.isNsfw ? ContentRating.ADULT : ContentRating.SAFE,
        source: this.source,
        chapters
      });
    }
    // Kotlin getChapters: list is already oldest-first in source markup; number = i+1.
    getChapters(doc) {
      const elements = this.queryAll(doc, this.selectChapter);
      return elements.map((li, i) => {
        const a = li.querySelector("a");
        if (!a)
          return null;
        const href = a.getAttribute("href");
        if (!href)
          return null;
        const relHref = this.toRelativeUrl(href);
        const title = (a.textContent || "").trim() || null;
        return new MangaChapter({
          id: relHref,
          url: relHref,
          title,
          number: i + 1,
          volume: 0,
          branch: null,
          uploadDate: 0,
          scanlator: null,
          source: this.source
        });
      }).filter((c) => c && c.url && !c.url.includes("#"));
    }
    // --- pages ---
    async getCdnHost() {
      try {
        const raw = await this.context.httpGet(this.toAbsoluteUrl(this.configJsPath), this);
        const marker = 'domain":["';
        const start = raw.indexOf(marker);
        if (start >= 0) {
          const rest = raw.slice(start + marker.length);
          const end = rest.indexOf('"]}');
          const host = (end >= 0 ? rest.slice(0, end) : rest).replace(/http:/g, "https:");
          if (host)
            return host;
        }
      } catch {
      }
      return `https://${this.domain}`;
    }
    async getPages(chapter) {
      const host = await this.getCdnHost();
      const html = await this.context.httpGet(this.toAbsoluteUrl(chapter.url), this);
      const marker = this.selectTestScriptMarker;
      const markerIdx = html.indexOf(marker);
      if (markerIdx < 0) {
        return [];
      }
      const afterImages = html.slice(markerIdx + marker.length);
      const arrOpen = afterImages.indexOf("[");
      let imagesPart = "";
      if (arrOpen >= 0) {
        const arrTail = afterImages.slice(arrOpen + 1);
        const arrClose = arrTail.indexOf("];");
        imagesPart = arrClose >= 0 ? arrTail.slice(0, arrClose) : arrTail.split("\n")[0];
      }
      const images = imagesPart.replace(/"/g, "").split(",").map((s) => s.trim()).filter((s) => s.length);
      let path = "";
      const pathMarker = 'chapterPath = "';
      const pathIdx = html.indexOf(pathMarker);
      if (pathIdx >= 0) {
        const afterPath = html.slice(pathIdx + pathMarker.length);
        const pathEnd = afterPath.indexOf('"');
        path = pathEnd >= 0 ? afterPath.slice(0, pathEnd) : "";
      }
      const pages = [];
      for (const it of images) {
        let imageUrl;
        if (it.startsWith("https:\\/\\/")) {
          imageUrl = it.replace(/\\/g, "");
        } else if (it.startsWith("http:\\/\\/")) {
          imageUrl = it.replace(/\\/g, "").replace("http:", "https:");
        } else if (it.startsWith("\\/")) {
          imageUrl = host + it.replace(/\\/g, "");
        } else if (it.startsWith("/")) {
          imageUrl = `${host}${it}`;
        } else {
          imageUrl = `${host}/${path}${it}`;
        }
        pages.push(new MangaPage({
          id: imageUrl,
          url: imageUrl,
          preview: null,
          source: this.source
        }));
      }
      return pages;
    }
  };

  // src/mangabox.js
  var MangaBoxParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 24) {
      super(context, source, domain, pageSize);
    }
    async getListPage(page, order, filter) {
      let url = `https://${this.domain}`;
      const isGg = this.domain.includes(".gg");
      if (filter.query && isGg) {
        const alias = String(filter.query).toLowerCase().trim().replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "");
        const jsonUrl = `https://${this.domain}/home/search/json?searchword=${encodeURIComponent(alias)}`;
        try {
          const j = await this.context.httpGet(jsonUrl, this);
          const arr = JSON.parse(j);
          return (Array.isArray(arr) ? arr : []).map((it) => {
            const href = it.url || it.link || "";
            const rel = this.toRelativeUrl(href);
            const title = String(it.name || it.title || "").replace(/<[^>]*>/g, "").trim();
            const cover = it.image || it.cover || "";
            return new Manga({
              id: rel,
              url: rel,
              publicUrl: href || this.toAbsoluteUrl(rel),
              coverUrl: cover && !cover.startsWith("data:") ? this.toAbsoluteUrl(cover) : "",
              title,
              source: this.source,
              contentRating: ContentRating.SAFE
            });
          }).filter((m) => m.url && m.title);
        } catch {
          return [];
        }
      }
      if (filter.query) {
        if (isGg) {
          url += `/search?keyword=${encodeURIComponent(filter.query)}&page=${page}`;
        } else {
          url += `/search/story/${encodeURIComponent(filter.query.replace(/\s+/g, "_"))}`;
          if (page > 1)
            url += `?page=${page}`;
        }
      } else {
        if (isGg) {
          let type = "latest-manga";
          if (order === SortOrder.POPULARITY)
            type = "hot-manga";
          if (order === SortOrder.NEWEST)
            type = "new-manga";
          url += `/manga-list/${type}?page=${page}`;
        } else {
          const isNato = this.domain.includes("manganato");
          if (isNato) {
            url += `/genre-all/${page}`;
            let type = "topview";
            if (order === SortOrder.NEWEST)
              type = "newest";
            if (order === SortOrder.UPDATED)
              type = "latest";
            url += `?type=${type}`;
          } else {
            url += `/manga_list?type=topview&category=all&state=all&page=${page}`;
            if (order === SortOrder.NEWEST)
              url = url.replace("topview", "newest");
            if (order === SortOrder.UPDATED)
              url = url.replace("topview", "latest");
          }
        }
      }
      const html = await this.context.httpGet(url, this);
      const doc = this.context.parseHTML(html);
      const elements = doc.querySelectorAll([
        ".list-truyen-item-wrap",
        ".content-genres-item",
        ".truyen-item",
        ".itemupdate",
        ".item",
        ".xem-nhieu-item",
        "div.story_item",
        "div.list-story-item"
      ].join(","));
      const mangaList = [];
      for (const el of Array.from(elements)) {
        const a = el.querySelector("h3 a, h2 a, a.genres-item-name, a.item-img, a.tooltip, a[title]");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href || href.includes("/chapter-"))
          continue;
        const relHref = this.toRelativeUrl(href);
        const img = el.querySelector("img");
        const titleEl = el.querySelector("h3, h2, .genres-item-name, .item-name, .title");
        let coverUrl = img ? img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "" : "";
        if (coverUrl && !coverUrl.startsWith("data:")) {
          coverUrl = this.toAbsoluteUrl(coverUrl);
        } else {
          coverUrl = "";
        }
        mangaList.push(new Manga({
          id: relHref,
          url: relHref,
          publicUrl: this.toAbsoluteUrl(relHref),
          coverUrl,
          title: (titleEl || a).textContent.trim(),
          source: this.source,
          contentRating: ContentRating.SAFE
        }));
      }
      return mangaList;
    }
    async getDetails(manga) {
      const url = this.toAbsoluteUrl(manga.url);
      const html = await this.context.httpGet(url, this);
      const doc = this.context.parseHTML(html);
      const title = doc.querySelector("h1, .story-info-right h1, .panel-story-info h1, .title-manga, .manga-info-text h1")?.textContent?.trim() || manga.title;
      const desc = doc.querySelector("#noidungm, .panel-story-info-description, .story-info-full, .content-manga, #panel-story-info-description, .description-content, .summary__content")?.textContent?.trim() || "";
      const img = doc.querySelector(".manga-info-pic img, .panel-story-info img, .info-image img, .cover img, .story-info-left img");
      let coverUrl = manga.coverUrl;
      if (img) {
        const raw = img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "";
        if (raw && !raw.startsWith("data:"))
          coverUrl = this.toAbsoluteUrl(raw);
      }
      let chapters = [];
      const isGg = this.domain.includes(".gg");
      const apiEl = doc.querySelector("#chapter-list-container[data-api-url], [data-comic-slug][data-api-url], [data-api-url][data-chapter-url-template]");
      if (isGg || apiEl) {
        const slugMatch = String(manga.url || "").match(/\/manga\/([^/?#]+)/);
        const slug = apiEl && apiEl.getAttribute("data-comic-slug") || (slugMatch ? slugMatch[1] : "");
        let apiBase = apiEl && apiEl.getAttribute("data-api-url") || "";
        if (apiBase.includes("__SLUG__"))
          apiBase = apiBase.replace(/__SLUG__/g, slug);
        if (!apiBase && slug)
          apiBase = `https://${this.domain}/api/manga/${slug}/chapters`;
        const urlTmpl = apiEl && apiEl.getAttribute("data-chapter-url-template") || "";
        if (apiBase && slug) {
          try {
            const collected = [];
            let offset = 0;
            for (let guard = 0; guard < 100; guard++) {
              const sep = apiBase.includes("?") ? "&" : "?";
              const apiJson = await this.context.httpGet(`${apiBase}${sep}offset=${offset}`, this);
              const parsed = JSON.parse(apiJson);
              const batch = parsed && parsed.data && parsed.data.chapters || [];
              if (!batch.length)
                break;
              collected.push(...batch);
              const pg = parsed && parsed.data && parsed.data.pagination || {};
              const limit = Number(pg.limit) || batch.length || 50;
              const total = Number(pg.total) || 0;
              offset += limit;
              const more = pg.has_more === true || (total > 0 ? collected.length < total : batch.length >= limit);
              if (!more)
                break;
            }
            chapters = collected.map((c, i) => {
              const chSlug = c.chapter_slug || c.slug || "";
              let chUrl;
              if (urlTmpl) {
                chUrl = urlTmpl.replace(/__MANGA__/g, slug).replace(/__CHAPTER__/g, chSlug);
              } else {
                chUrl = `https://${this.domain}/manga/${slug}/${chSlug}`;
              }
              const rel = this.toRelativeUrl(chUrl);
              return new MangaChapter({
                id: rel,
                url: rel,
                title: c.chapter_name || c.name || `Chapter ${c.chapter_num != null ? c.chapter_num : i + 1}`,
                number: Number(c.chapter_num != null ? c.chapter_num : c.number) || i + 1,
                source: this.source
              });
            }).filter((c) => c.url);
            chapters.reverse();
          } catch {
            chapters = [];
          }
        }
      }
      if (!chapters.length) {
        let elements = Array.from(doc.querySelectorAll([
          ".chapter-list .row",
          ".row-content-chapter li",
          ".chapter-list li",
          ".row-content-chapter .a-h",
          ".panel-story-chapter-list li",
          ".panel-story-chapter-list .a-h",
          "ul.list-chapter li",
          ".list-chapter .row",
          ".chapter-list div.row"
        ].join(",")));
        if (!elements.length) {
          const idEl = doc.querySelector("a[data-id], #manga_id, input[name='manga_id'], .bookmark_check[data-id]");
          const mangaId = idEl ? idEl.getAttribute("data-id") || idEl.value : null;
          if (mangaId) {
            try {
              const ajaxUrl = `https://${this.domain}/ajax/chapter/list?manga_id=${mangaId}`;
              const ajaxHtml = await this.context.httpGet(ajaxUrl, this);
              const ajaxDoc = this.context.parseHTML(ajaxHtml);
              elements = Array.from(ajaxDoc.querySelectorAll("li, .row, a"));
            } catch {
            }
          }
        }
        if (!elements.length) {
          elements = Array.from(doc.querySelectorAll("a[href*='/chapter-']")).map((a) => a.closest("li, div.row, div.item, .a-h") || a);
        }
        chapters = elements.map((el, i) => {
          const a = el.tagName === "A" ? el : el.querySelector("a");
          const href = a?.getAttribute("href");
          if (!href)
            return null;
          const relHref = this.toRelativeUrl(href);
          return new MangaChapter({
            id: relHref,
            url: relHref,
            title: a?.textContent?.trim() || `Chapter ${i + 1}`,
            number: i + 1,
            source: this.source
          });
        }).filter((c) => c && c.url && !c.url.includes("javascript:void")).reverse();
      }
      const genreEls = doc.querySelectorAll(
        ".genres-wrap .genre-list a, .panel-story-info .table-value a[href*='/genre/'], .story-info-right .table-value a[href*='/genre/'], li.genres a[href*='/genre/']"
      );
      const seenTag = {};
      const tags = Array.from(genreEls).map((a) => {
        const title2 = (a.textContent || "").trim();
        const href = a.getAttribute("href") || "";
        const key = (href.match(/\/genre\/([^/?#]+)/) || [])[1] || title2.toLowerCase();
        return { title: title2, key };
      }).filter((g) => {
        const t = g.title.toLowerCase();
        if (!g.title || t === "all" || t === "completed" || t === "ongoing" || t === "latest")
          return false;
        if (seenTag[g.key])
          return false;
        seenTag[g.key] = true;
        return true;
      });
      return new Manga({
        ...manga,
        title,
        coverUrl,
        description: desc,
        tags,
        chapters
      });
    }
    async getPages(chapter) {
      const url = this.toAbsoluteUrl(chapter.url);
      const html = await this.context.httpGet(url, this);
      const imagesMatch = html.match(/chapterImages\s*=\s*(\[[\s\S]*?\])/);
      if (imagesMatch) {
        let paths = [];
        try {
          paths = JSON.parse(imagesMatch[1].replace(/\\\//g, "/"));
        } catch {
          paths = [];
        }
        if (paths.length) {
          let base = "";
          const cdnMatch = html.match(/cdns\s*=\s*(\[[\s\S]*?\])/);
          if (cdnMatch) {
            try {
              const cdns = JSON.parse(cdnMatch[1].replace(/\\\//g, "/"));
              base = cdns && cdns[0] || "";
            } catch {
            }
          }
          if (!base)
            base = "https://imgs-2.2xstorage.com";
          base = base.replace(/\/+$/, "");
          return paths.map((p) => {
            const s = String(p).replace(/\\\//g, "/").replace(/^\/+/, "");
            const full = /^https?:\/\//.test(s) ? s : `${base}/${s}`;
            return new MangaPage({
              id: full,
              url: full,
              source: this.source,
              headers: { "Referer": `https://${this.domain}/` }
            });
          }).filter((pg) => pg.url && !pg.url.includes("/banners-web/") && !pg.url.includes("yougetwhatyoupayfor") && !pg.url.includes("/thumb/"));
        }
      }
      const doc = this.context.parseHTML(html);
      const images = doc.querySelectorAll([
        ".container-chapter-reader img",
        ".v-content img",
        ".reader-content img",
        ".chapter-content img",
        "#v-content img"
      ].join(","));
      return Array.from(images).map((img) => {
        const imageUrl = img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "";
        return new MangaPage({
          id: imageUrl,
          url: imageUrl,
          source: this.source,
          headers: { "Referer": `https://${this.domain}/` }
        });
      }).filter((p) => p.url && !p.url.includes("ads") && !p.url.includes("logo") && !p.url.startsWith("data:"));
    }
  };

  // src/mangafire.js
  var VRF = (() => {
    const dec = (b64) => {
      const bin = atob(b64);
      const a = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++)
        a[i] = bin.charCodeAt(i);
      return a;
    };
    const rc4 = (key, input) => {
      const s = new Uint8Array(256);
      for (let i2 = 0; i2 < 256; i2++)
        s[i2] = i2;
      let j = 0;
      for (let i2 = 0; i2 < 256; i2++) {
        j = j + s[i2] + key[i2 % key.length] & 255;
        const t = s[i2];
        s[i2] = s[j];
        s[j] = t;
      }
      const out = new Uint8Array(input.length);
      let i = 0;
      j = 0;
      for (let y = 0; y < input.length; y++) {
        i = i + 1 & 255;
        j = j + s[i] & 255;
        const t = s[i];
        s[i] = s[j];
        s[j] = t;
        out[y] = input[y] ^ s[s[i] + s[j] & 255];
      }
      return out;
    };
    const add8 = (n) => (c) => c + n & 255;
    const sub8 = (n) => (c) => c - n + 256 & 255;
    const rotl8 = (n) => (c) => (c << n | c >>> 8 - n) & 255;
    const rotr8 = (n) => (c) => (c >>> n | c << 8 - n) & 255;
    const sC = [sub8(223), rotr8(4), rotr8(4), add8(234), rotr8(7), rotr8(2), rotr8(7), sub8(223), rotr8(7), rotr8(6)];
    const sY = [add8(19), rotr8(7), add8(19), rotr8(6), add8(19), rotr8(1), add8(19), rotr8(6), rotr8(7), rotr8(4)];
    const sB = [sub8(223), rotr8(1), add8(19), sub8(223), rotl8(2), sub8(223), add8(19), rotl8(1), rotl8(2), rotl8(1)];
    const sJ = [add8(19), rotl8(1), rotl8(1), rotr8(1), add8(234), rotl8(1), sub8(223), rotl8(6), rotl8(4), rotl8(1)];
    const sE = [rotr8(1), rotl8(1), rotl8(6), rotr8(1), rotl8(2), rotr8(4), rotl8(1), rotl8(1), sub8(223), rotl8(2)];
    const transform = (input, seed, prefix, sch) => {
      const out = [];
      for (let i = 0; i < input.length; i++) {
        if (i < prefix.length)
          out.push(prefix[i]);
        out.push(sch[i % 10]((input[i] ^ seed[i % 32]) & 255) & 255);
      }
      return new Uint8Array(out);
    };
    const K = { l: "FgxyJUQDPUGSzwbAq/ToWn4/e8jYzvabE+dLMb1XU1o=", g: "CQx3CLwswJAnM1VxOqX+y+f3eUns03ulxv8Z+0gUyik=", B: "fAS+otFLkKsKAJzu3yU+rGOlbbFVq+u+LaS6+s1eCJs=", m: "Oy45fQVK9kq9019+VysXVlz1F9S1YwYKgXyzGlZrijo=", F: "aoDIdXezm2l3HrcnQdkPJTDT8+W6mcl2/02ewBHfPzg=" };
    const SD = { A: "yH6MXnMEcDVWO/9a6P9W92BAh1eRLVFxFlWTHUqQ474=", V: "RK7y4dZ0azs9Uqz+bbFB46Bx2K9EHg74ndxknY9uknA=", N: "rqr9HeTQOg8TlFiIGZpJaxcvAaKHwMwrkqojJCpcvoc=", P: "/4GPpmZXYpn5RpkP7FC/dt8SXz7W30nUZTe8wb+3xmU=", k: "wsSGSBXKWA9q1oDJpjtJddVxH+evCfL5SO9HZnUDFU8=" };
    const PK = { O: "l9PavRg=", v: "Ml2v7ag1Jg==", L: "i/Va0UxrbMo=", p: "WFjKAHGEkQM=", W: "5Rr27rWd" };
    const b64url = (b) => {
      let s = "";
      for (let i = 0; i < b.length; i++)
        s += String.fromCharCode(b[i]);
      return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
    };
    return {
      generate(input) {
        let b = new TextEncoder().encode(encodeURIComponent(String(input)));
        b = rc4(dec(K.l), b);
        b = transform(b, dec(SD.A), dec(PK.O), sC);
        b = rc4(dec(K.g), b);
        b = transform(b, dec(SD.V), dec(PK.v), sY);
        b = rc4(dec(K.B), b);
        b = transform(b, dec(SD.N), dec(PK.L), sB);
        b = rc4(dec(K.m), b);
        b = transform(b, dec(SD.P), dec(PK.p), sJ);
        b = rc4(dec(K.F), b);
        b = transform(b, dec(SD.k), dec(PK.W), sE);
        return b64url(b);
      }
    };
  })();
  var MangaFireParser = class extends BaseParser {
    constructor(context, source, domain, pageSize = 30) {
      super(context, source, domain, pageSize);
    }
    get lang() {
      return this.source && (this.source.locale || this.source.lang) || "en";
    }
    sortValue(order) {
      switch (order) {
        case SortOrder.UPDATED:
          return "recently_updated";
        case SortOrder.POPULARITY:
          return "most_viewed";
        case SortOrder.RATING:
          return "scores";
        case SortOrder.NEWEST:
          return "release_date";
        case SortOrder.ALPHABETICAL:
          return "title_az";
        case SortOrder.RELEVANCE:
          return "most_relevance";
        default:
          return "most_viewed";
      }
    }
    async getJson(url) {
      return JSON.parse(await this.context.httpGet(url, this));
    }
    async getListPage(page, order, filter) {
      filter = filter || {};
      const lang = this.lang;
      let url = `https://${this.domain}/filter?page=${page}&language[]=${encodeURIComponent(lang)}`;
      if (filter.query) {
        const q = filter.query.trim();
        const kw = q.replace(/\s+/g, "+");
        url += `&keyword=${encodeURIComponent(kw).replace(/%2B/g, "+")}`;
        url += `&vrf=${encodeURIComponent(VRF.generate(q))}`;
        url += `&sort=most_relevance`;
      } else {
        url += `&sort=${this.sortValue(order)}`;
      }
      const html = await this.context.httpGet(url, this);
      const doc = this.context.parseHTML(html);
      const items = doc.querySelectorAll(".original.card-lg .unit .inner, .original .unit .inner, .unit .inner");
      const out = [];
      for (const el of Array.from(items)) {
        const a = el.querySelector(".info > a, .info a");
        if (!a)
          continue;
        const href = a.getAttribute("href");
        if (!href || !href.includes("/manga/"))
          continue;
        const img = el.querySelector("img");
        const cover = img ? img.getAttribute("src") || img.getAttribute("data-src") || "" : "";
        const rel = this.toRelativeUrl(href);
        out.push(new Manga({
          id: rel,
          url: rel,
          publicUrl: this.toAbsoluteUrl(rel),
          title: (a.textContent || "").trim(),
          coverUrl: cover ? this.toAbsoluteUrl(cover) : "",
          source: this.source,
          contentRating: ContentRating.SAFE
        }));
      }
      return out;
    }
    async getDetails(manga) {
      const url = this.toAbsoluteUrl(manga.url);
      const html = await this.context.httpGet(url, this);
      const doc = this.context.parseHTML(html);
      const title = doc.querySelector(".info > h1, .info h1, h1[itemprop='name']")?.textContent?.trim() || manga.title;
      const coverRaw = doc.querySelector("div.manga-detail div.poster img, .poster img")?.getAttribute("src") || "";
      const descEl = doc.querySelector("#synopsis div.modal-content, #synopsis .modal-content, #synopsis, .description");
      const description = descEl ? (descEl.textContent || "").trim() : "";
      const genreEls = doc.querySelectorAll("div.meta a[href*='/genre/'], .meta a[href*='/genre/']");
      const tags = Array.from(genreEls).map((a) => {
        const t = (a.textContent || "").trim();
        const key = ((a.getAttribute("href") || "").match(/\/genre\/([^/?#]+)/) || [])[1] || t.toLowerCase();
        return { title: t, key };
      }).filter((g) => g.title);
      let chapters = [];
      try {
        chapters = await this.getChapters(manga.url, doc);
      } catch {
        chapters = [];
      }
      return new Manga({
        ...manga,
        title,
        coverUrl: coverRaw ? this.toAbsoluteUrl(coverRaw) : manga.coverUrl,
        description,
        tags,
        chapters
      });
    }
    async getChapters(mangaUrl, doc) {
      const lang = this.lang;
      const mangaId = String(mangaUrl).substring(String(mangaUrl).lastIndexOf(".") + 1);
      const availableTypes = Array.from(doc.querySelectorAll(".chapvol-tab > a")).map((a) => a.getAttribute("data-name")).filter(Boolean);
      const branches = [];
      for (const tc of Array.from(doc.querySelectorAll(".m-list div.tab-content"))) {
        const type = tc.getAttribute("data-name");
        for (const item of Array.from(tc.querySelectorAll(".list-menu .dropdown-item"))) {
          branches.push({ type, langCode: (item.getAttribute("data-code") || "").toLowerCase(), langTitle: item.getAttribute("data-title") || "" });
        }
      }
      let wanted = branches.filter((b) => b.langCode === lang && (availableTypes.length === 0 || availableTypes.includes(b.type)));
      if (!wanted.length)
        wanted = [{ type: "chapter", langCode: lang, langTitle: lang.toUpperCase() }];
      const chapterBranches = wanted.filter((b) => b.type === "chapter");
      const useBranches = chapterBranches.length ? chapterBranches : wanted;
      let all = [];
      for (const b of useBranches) {
        const br = await this.getChaptersBranch(mangaId, b);
        all = all.concat(br);
      }
      return all;
    }
    async getChaptersBranch(mangaId, branch) {
      const readVrf = VRF.generate(`${mangaId}@${branch.type}@${branch.langCode}`);
      const j = await this.getJson(`https://${this.domain}/ajax/read/${mangaId}/${branch.type}/${branch.langCode}?vrf=${encodeURIComponent(readVrf)}`);
      const listHtml = j && j.result && j.result.html || "";
      const listDoc = this.context.parseHTML(listHtml);
      const aEls = Array.from(listDoc.querySelectorAll("ul li a"));
      let dateAs = [];
      if (branch.type === "chapter") {
        try {
          const jm = await this.getJson(`https://${this.domain}/ajax/manga/${mangaId}/${branch.type}/${branch.langCode}`);
          const mhtml = jm && jm.result || "";
          if (typeof mhtml === "string")
            dateAs = Array.from(this.context.parseHTML(mhtml).querySelectorAll("ul li a"));
        } catch {
        }
      }
      const chapters = aEls.map((a, i) => {
        const chapterId = a.getAttribute("data-id");
        if (!chapterId)
          return null;
        const number = parseFloat(a.getAttribute("data-number"));
        const titleAttr = (a.getAttribute("title") || "").trim();
        const chUrl = `${mangaId}/${branch.type}/${branch.langCode}/${chapterId}`;
        return new MangaChapter({
          id: chUrl,
          url: chUrl,
          title: titleAttr || `${branch.type === "volume" ? "Volume" : "Chapter"} ${a.getAttribute("data-number") || i + 1}`,
          number: Number.isFinite(number) ? number : i + 1,
          source: this.source
        });
      }).filter(Boolean);
      chapters.reverse();
      return chapters;
    }
    async getPages(chapter) {
      const parts = String(chapter.url).split("/");
      const type = parts[1] || "chapter";
      const chapterId = parts[3] || parts[parts.length - 1];
      const vrf = VRF.generate(`${type}@${chapterId}`);
      const j = await this.getJson(`https://${this.domain}/ajax/read/${type}/${chapterId}?vrf=${encodeURIComponent(vrf)}`);
      const images = j && j.result && j.result.images || [];
      return images.map((img) => {
        const u = Array.isArray(img) ? img[0] : img && img.url;
        const offset = Array.isArray(img) ? img[2] : img && img.offset;
        const full = offset && offset >= 1 ? `${u}#scrambled_${offset}` : u;
        return new MangaPage({
          id: u,
          url: full,
          source: this.source,
          headers: { "Referer": `https://${this.domain}/` }
        });
      }).filter((p) => p.url);
    }
  };

  // src/sources.json
  var sources_default = [
    {
      id: "BANANASCAN_COM",
      className: "BananaScan",
      title: "BananaScan.Com",
      locale: "en",
      domain: "bananascans.com",
      family: "MmrcmsParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "DANKE",
      className: "Danke",
      title: "DankeFursLesen",
      locale: "en",
      domain: "danke.moe",
      family: "GuyaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "FMTEAM",
      className: "FmTeam",
      title: "FmTeam",
      locale: "fr",
      domain: "fmteam.fr",
      family: "PizzaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "HNISCANTRAD",
      className: "HniScantrad",
      title: "HniScantrad",
      locale: "fr",
      domain: "hni-scantrad.net",
      family: "PizzaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "TUTTOANIMEMANGA",
      className: "TuttoAnimeManga",
      title: "TuttoAnimeManga",
      locale: "it",
      domain: "tuttoanimemanga.net",
      family: "PizzaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "ARCRELIGHT",
      className: "ArcRelight",
      title: "Arc-Relight",
      locale: "en",
      domain: "arc-relight.com",
      family: "MangAdventureParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "ASSORTEDSCANS",
      className: "AssortedScans",
      title: "AssortedScans",
      locale: "en",
      domain: "assortedscans.com",
      family: "MangAdventureParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "GTOTHEGREATSITE",
      className: "GtoTheGreatSite",
      title: "GtoTheGreatSite",
      locale: "it",
      domain: "reader.gtothegreatsite.net",
      family: "PizzaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "GUYACUBARI",
      className: "GuyaCubari",
      title: "GuyaCubari",
      locale: "en",
      domain: "guya.cubari.moe",
      family: "GuyaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "HASTATEAM",
      className: "HastaTeam",
      title: "HastaTeamDdt",
      locale: "it",
      domain: "ddt.hastateam.com",
      family: "PizzaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "HASTATEAM_READER",
      className: "HastaTeamReader",
      title: "HastaTeamReader",
      locale: "it",
      domain: "reader.hastateam.com",
      family: "PizzaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "HIVECOMIC",
      className: "HiveComic",
      title: "HiveComic",
      locale: "en",
      domain: "hivetoons.org",
      family: "IkenParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "LUPITEAM",
      className: "LupiTeam",
      title: "LupiTeam",
      locale: "it",
      domain: "lupiteam.net",
      family: "PizzaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "MAGUSMANGA",
      className: "MagusToon",
      title: "MagusToon",
      locale: "en",
      domain: "magustoon.org",
      family: "IkenParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "NYXSCANS",
      className: "NyxScans",
      title: "Nyx Scans",
      locale: "en",
      domain: "nyxscans.com",
      family: "IkenParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "PHOENIXSCANS",
      className: "PhoenixScans",
      title: "PhoenixScans",
      locale: "it",
      domain: "www.phoenixscans.com",
      family: "PizzaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "VORTEXSCANS",
      className: "VortexScans",
      title: "VortexScans",
      locale: "en",
      domain: "vortexscans.org",
      family: "IkenParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "YAOIX3",
      className: "YaoiX3",
      title: "3XYaoi",
      locale: "pt-BR",
      domain: "3xyaoi.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        listUrl: "bl/",
        datePattern: "dd/MM/yyyy",
        tagPrefix: "genero/"
      }
    },
    {
      id: "SCANS4U",
      className: "Scans4u",
      title: "4uScans",
      locale: "ar",
      domain: "4uscans.com",
      family: "KeyoappParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "APENASMAISUMYAOI",
      className: "ApenasmaisumYaoi",
      title: "Apenasmaisum Yaoi",
      locale: "pt-BR",
      domain: "apenasmaisumyaoi.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {}
    },
    {
      id: "APKOMIK",
      className: "Apkomik",
      title: "Apkomik",
      locale: "id",
      domain: "01.apkomik.com",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "ARABSHENTAI",
      className: "ArabsHentai",
      title: "Arabs Hentai",
      locale: "ar",
      domain: "arabshentai.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        listUrl: "manga/",
        datePattern: "yyyy-MM-dd",
        selectChapter: "#chapter-list a[href*='/manga/'], .oneshot-reader",
        selectPage: ".chapter_image img.wp-manga-chapter-img",
        selectDesc: "#manga-info .wp-content p",
        selectState: "#manga-info div b:contains(\u062D\u0627\u0644\u0629 \u0627\u0644\u0645\u0627\u0646\u062C\u0627)",
        withoutAjax: true
      }
    },
    {
      id: "ARCURAFANSUB",
      className: "ArcuraFansub",
      title: "ArcuraFansub",
      locale: "tr",
      domain: "arcurafansub.com",
      family: "MangaReaderParser",
      isNsfw: true,
      overrides: {
        listUrl: "/seri"
      }
    },
    {
      id: "BERSERKSCAN",
      className: "BerserkScan",
      title: "BerserkScan",
      locale: "fr",
      domain: "berserkscan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "BLUELOCKSCAN",
      className: "BlueLockScan",
      title: "BlueLockScan",
      locale: "fr",
      domain: "bluelockscan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "CAFECOMYAOI",
      className: "CafecomYaoi",
      title: "CafecomYaoi",
      locale: "pt-BR",
      domain: "cafecomyaoi.com.br",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "dd/MM/yyyy"
      }
    },
    {
      id: "CHAINSAWMANSCAN",
      className: "ChainsawManScan",
      title: "ChainsawManScan",
      locale: "fr",
      domain: "chainsawman-scan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "COSMIC_SCANS",
      className: "CosmicScans",
      title: "CosmicScans.id",
      locale: "id",
      domain: "lc1.cosmicscans.to",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "DEMONSLAYERSCAN",
      className: "DemonSlayerScan",
      title: "DemonSlayerScan",
      locale: "fr",
      domain: "demonslayerscan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "DOUJINKU",
      className: "DoujinKu",
      title: "DoujinKu",
      locale: "id",
      domain: "doujinku.org",
      family: "MangaReaderParser",
      isNsfw: true,
      overrides: {}
    },
    {
      id: "DRAGONTEA",
      className: "DragonTea",
      title: "DragonTea",
      locale: "en",
      domain: "dragontea.ink",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "MM/dd/yyyy"
      }
    },
    {
      id: "DRSTONE",
      className: "DrStone",
      title: "DrStone",
      locale: "fr",
      domain: "drstone.fr",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "EMPERORSCAN",
      className: "EmperorScan",
      title: "EmperorScan",
      locale: "es",
      domain: "imperiomanhua.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "MMMM dd, yyyy",
        selectDesc: "div.summary_content div.post-content_item:has(h5:contains(Sinopsis)) div",
        withoutAjax: true
      }
    },
    {
      id: "ERO18X",
      className: "Ero18x",
      title: "Ero18x",
      locale: "all",
      domain: "ero18x.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "MM/dd"
      }
    },
    {
      id: "EVILMANGA",
      className: "EvilManga",
      title: "EvilManga",
      locale: "cs",
      domain: "evil-manga.eu",
      family: "MangaReaderParser",
      isNsfw: true,
      overrides: {
        datePattern: "d MMMM, yyyy"
      }
    },
    {
      id: "EZMANGA",
      className: "EzManga",
      title: "EzManga",
      locale: "en",
      domain: "ezmanga.org",
      family: "IkenParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "FBSQUADS",
      className: "Fbsquads",
      title: "FbSquads",
      locale: "pt-BR",
      domain: "fbsquadx.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {}
    },
    {
      id: "GAFELAND",
      className: "Gafeland",
      title: "Gafeland",
      locale: "tr",
      domain: "www.gafeland.com",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "GAIATOON",
      className: "GaiaToon",
      title: "GaiaToon",
      locale: "tr",
      domain: "gaiatoon.com",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "GOLGEBAHCESI",
      className: "Golgebahcesi",
      title: "GolgeBahcesi",
      locale: "tr",
      domain: "golgebahcesi.com",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "HAIKYUU",
      className: "Haikyuu",
      title: "Haikyuu",
      locale: "fr",
      domain: "haikyuu.fr",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "HELLSPARADISESCAN",
      className: "HellsParadiseScan",
      title: "HellsParadiseScan",
      locale: "fr",
      domain: "hellsparadisescan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "HENTAISCANTRADVF",
      className: "HentaiScantradVf",
      title: "Hentai-Scantrad",
      locale: "fr",
      domain: "hentai.scantrad-vf.cc",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "d MMMM, yyyy"
      }
    },
    {
      id: "HENTAIZONE",
      className: "Hentaizone",
      title: "HentaiZone",
      locale: "fr",
      domain: "hentaizone.xyz",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "MMM d, yyyy"
      }
    },
    {
      id: "HUNTERXHUNTERSCAN",
      className: "HunterXHunterScan",
      title: "HunterXHunterScan",
      locale: "fr",
      domain: "hunterxhunterscan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "HWAGO",
      className: "Hwago",
      title: "Hwago",
      locale: "id",
      domain: "01.hwago.xyz",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "d MMMM yyyy"
      }
    },
    {
      id: "KINGDOMSCAN",
      className: "KingdomScan",
      title: "KingdomScan",
      locale: "fr",
      domain: "kingdomscan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "KISSMANGA",
      className: "KissManga",
      title: "KissManga",
      locale: "en",
      domain: "kissmanga.in",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        listUrl: "mangalist/",
        datePattern: "MMMM dd, yyyy"
      }
    },
    {
      id: "KLZ9",
      className: "Klz9",
      title: "Klz9",
      locale: "ja",
      domain: "klz9.com",
      family: "FmreaderParser",
      isNsfw: false,
      overrides: {
        selectChapter: "tr",
        selectPage: "img",
        selectDesc: "div.row:contains(Description) p"
      }
    },
    {
      id: "MANHWAINDOICU",
      className: "ManhwaIndoIcu",
      title: "KomikCinta",
      locale: "id",
      domain: "komikdewasa.art",
      family: "MangaReaderParser",
      isNsfw: true,
      overrides: {
        listUrl: "/komik"
      }
    },
    {
      id: "KOMIKDEWASA_ONLINE",
      className: "KomikDewasa",
      title: "KomikDewasa.Online",
      locale: "id",
      domain: "komikdewasa.art",
      family: "MangaReaderParser",
      isNsfw: true,
      overrides: {}
    },
    {
      id: "KOMIKZOID",
      className: "KomikzoId",
      title: "KomikzoId",
      locale: "id",
      domain: "komikzoid.id",
      family: "AnimeBootstrapParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "KUNMANGA",
      className: "KunManga",
      title: "KunManga",
      locale: "en",
      domain: "kunmanga.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        withoutAjax: true
      }
    },
    {
      id: "LEGENDSCANLATIONS",
      className: "LegendScanlations",
      title: "LegendScanlations",
      locale: "es",
      domain: "escaneodeleyendas.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "dd/MM/yyyy"
      }
    },
    {
      id: "LEKMANGAORG",
      className: "LekMangaOrg",
      title: "LekManga.org",
      locale: "ar",
      domain: "lekmanga.org",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        listUrl: "readcomics/"
      }
    },
    {
      id: "LERMANGAS",
      className: "Lermangas",
      title: "Lermangas",
      locale: "pt-BR",
      domain: "lermangas.me",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "dd 'de' MMMMM 'de' yyyy"
      }
    },
    {
      id: "LUMOSKOMIK",
      className: "LumosKomik",
      title: "LumosKomik",
      locale: "id",
      domain: "02.lumosgg.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        listUrl: "komik/",
        datePattern: "dd MMMM yyyy",
        tagPrefix: "genre/"
      }
    },
    {
      id: "MANGASTARZ",
      className: "MangaStarz",
      title: "Manga-Starz",
      locale: "ar",
      domain: "manga-starz.net",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "d MMMM\u060C yyyy",
        withoutAjax: true
      }
    },
    {
      id: "MANGA168",
      className: "Manga168",
      title: "Manga168",
      locale: "th",
      domain: "manga168.net",
      family: "MangaReaderParser",
      isNsfw: true,
      overrides: {}
    },
    {
      id: "MANGAGG",
      className: "Mangagg",
      title: "MangaGg",
      locale: "en",
      domain: "mangagg.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "MM/dd/yyyy",
        tagPrefix: "genre/"
      }
    },
    {
      id: "MANGA_SCANTRAD",
      className: "MangaScantrad",
      title: "MangaScantrad.io",
      locale: "fr",
      domain: "manga-scantrad.io",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "d MMMM yyyy"
      }
    },
    {
      id: "MANGASEHRI",
      className: "Mangasehri",
      title: "MangaSehri.com",
      locale: "tr",
      domain: "manga-sehri.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "dd/MM/yyyy"
      }
    },
    {
      id: "MANGASEHRINET",
      className: "MangaSehriNet",
      title: "MangaSehri.net",
      locale: "tr",
      domain: "manga-sehri.net",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "dd MMMM yyyy"
      }
    },
    {
      id: "MANGASNOSEKAI",
      className: "MangasNoSekai",
      title: "MangasNoSekai",
      locale: "es",
      domain: "mangasnosekai.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "MANGASORIGINES",
      className: "MangasOrigines",
      title: "MangasOrigines.fr",
      locale: "fr",
      domain: "mangas-origines.fr",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        listUrl: "oeuvre/",
        datePattern: "dd/MM/yyyy",
        tagPrefix: "manga-genres/"
      }
    },
    {
      id: "TOPMANHUA",
      className: "TopManhua",
      title: "ManhuaTop",
      locale: "en",
      domain: "manhuatop.org",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        listUrl: "manhua/",
        datePattern: "MM/dd/yyyy",
        tagPrefix: "manhua-genre/"
      }
    },
    {
      id: "MANHUAZONGHE",
      className: "ManhuaZonghe",
      title: "ManhuaZonghe",
      locale: "en",
      domain: "www.manhuazonghe.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        listUrl: "manhua/",
        tagPrefix: "genre/"
      }
    },
    {
      id: "MANHWA_ES",
      className: "ManhwaEs",
      title: "Manhwa-Es",
      locale: "es",
      domain: "manhwa-es.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "MM/dd"
      }
    },
    {
      id: "MANHWACLAN",
      className: "ManhwaClan",
      title: "ManhwaClan",
      locale: "en",
      domain: "manhwaclan.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "MMMM dd, yyyy"
      }
    },
    {
      id: "MANHWALAND",
      className: "ManhwaLand",
      title: "ManhwaLand.vip",
      locale: "id",
      domain: "www.manhwaland.baby",
      family: "MangaReaderParser",
      isNsfw: true,
      overrides: {
        datePattern: "MMM d, yyyy"
      }
    },
    {
      id: "MANHWALATINO",
      className: "ManhwaLatino",
      title: "ManhwaLatino",
      locale: "es",
      domain: "manhwa-latino.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "MM/dd",
        selectPage: "div.page-break img.wp-manga-chapter-img"
      }
    },
    {
      id: "MANHWAONLINE",
      className: "ManhwaOnline",
      title: "ManhwaOnline",
      locale: "es",
      domain: "manhwa-online.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "MMMM dd, yyyy"
      }
    },
    {
      id: "MANHWARAW",
      className: "ManhwaRaw",
      title: "ManhwaRaw",
      locale: "all",
      domain: "manhwa-raw.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "MM/dd"
      }
    },
    {
      id: "MANHWATOP",
      className: "ManhwaTop",
      title: "ManhwaTop",
      locale: "en",
      domain: "manhwatop.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        postReq: true
      }
    },
    {
      id: "MINITWOSCAN",
      className: "MiniTwoScan",
      title: "MiniTwoScan",
      locale: "pt-BR",
      domain: "minitwoscan.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        postReq: true,
        withoutAjax: true
      }
    },
    {
      id: "MUGIWARASOFICIAL",
      className: "MugiwarasOficial",
      title: "MugiwarasOficial",
      locale: "pt-BR",
      domain: "mugiwarasoficial.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "MYHEROACADEMIASCAN",
      className: "MyHeroacAdemiaScan",
      title: "MyHeroacAdemiaScan",
      locale: "fr",
      domain: "myheroacademiascan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "NECROSCANS",
      className: "NecroScans",
      title: "NecroScans",
      locale: "en",
      domain: "necroscans.com",
      family: "KeyoappParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "NETTRUYEN",
      className: "NetTruyen",
      title: "NetTruyen",
      locale: "vi",
      domain: "nettruyenar.com",
      family: "WpComicsParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "NEUMANGA",
      className: "NeuManga",
      title: "NeuManga.xyz",
      locale: "id",
      domain: "neumanga.id",
      family: "AnimeBootstrapParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "NHATTRUYENVN",
      className: "NhatTruyenVN",
      title: "NhatTruyenVN",
      locale: "vi",
      domain: "nhattruyenqq.com",
      family: "WpComicsParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "NOCSUMMER",
      className: "Nocsummer",
      title: "NocturneSummer",
      locale: "pt-BR",
      domain: "nocfsb.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "dd 'de' MMMMM 'de' yyyy"
      }
    },
    {
      id: "NOVELCROW",
      className: "Novelcrow",
      title: "NovelCrow",
      locale: "en",
      domain: "novelcrow.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        tagPrefix: "comic-genre/"
      }
    },
    {
      id: "ONEPUNCHMANSCAN",
      className: "OnePunchManScan",
      title: "OnePunchManScan",
      locale: "fr",
      domain: "onepunchmanscan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "PATIMANGA",
      className: "PatiManga",
      title: "PatiManga",
      locale: "tr",
      domain: "www.patimanga.com",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "RAGNARSCANS",
      className: "Ragnarscans",
      title: "Ragnarscans",
      locale: "tr",
      domain: "ragnarscans.com",
      family: "InitMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "RAIJINSCANS",
      className: "RaijinScans",
      title: "RaijinScans",
      locale: "fr",
      domain: "raijin-scans.fr",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "dd/MM/yyyy",
        tagPrefix: "genre/",
        selectChapter: "ul.scroll-sm li.item",
        selectPage: "div.protected-image-data",
        selectDesc: "div.description-content",
        selectState: "div.stat-item:has(span:contains(\xC9tat du titre)) span.manga",
        withoutAjax: true
      }
    },
    {
      id: "READCOMICSONLINE",
      className: "ReadComicsOnline",
      title: "ReadComicsOnline.ru",
      locale: "en",
      domain: "readcomicsonline.ru",
      family: "MmrcmsParser",
      isNsfw: false,
      overrides: {
        selectState: "dt:contains(Status)"
      }
    },
    {
      id: "REZOSCANS",
      className: "RezoScans",
      title: "RezoScans",
      locale: "en",
      domain: "rezoscans.com",
      family: "KeyoappParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "SAKAMOTODAYS",
      className: "SakamotoDays",
      title: "SakamotoDays",
      locale: "fr",
      domain: "sakamotodays.fr",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "SCANBORUTO",
      className: "ScanBoruto",
      title: "ScanBoruto",
      locale: "fr",
      domain: "scanboruto.fr",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "SCANJUJUTSUKAISEN",
      className: "ScanJujutsuKaisen",
      title: "ScanJujutsuKaisen",
      locale: "fr",
      domain: "scanjujutsukaisen.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "SCANVF",
      className: "ScanVf",
      title: "ScanVf",
      locale: "fr",
      domain: "www.scan-vf.net",
      family: "MmrcmsParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "SEKTEKOMIK",
      className: "SekteKomik",
      title: "SekteKomik",
      locale: "id",
      domain: "sektekomik.id",
      family: "AnimeBootstrapParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "SHIRAKAMI",
      className: "Shirakami",
      title: "Shirakami",
      locale: "id",
      domain: "shirakami.xyz",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "SHIRO_DOUJIN",
      className: "ShiroDoujin",
      title: "ShiroDoujin",
      locale: "id",
      domain: "shirodoujin.com",
      family: "ZMangaParser",
      isNsfw: true,
      overrides: {}
    },
    {
      id: "SNKSCAN",
      className: "SnkScan",
      title: "SnkScan",
      locale: "fr",
      domain: "snkscan.com",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "SOULSCANS",
      className: "SoulScans",
      title: "SoulScans",
      locale: "id",
      domain: "soulscans.my.id",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {
        datePattern: "MMM d, yyyy"
      }
    },
    {
      id: "SUSHISCAN",
      className: "SushiScan",
      title: "SushiScan.Net",
      locale: "fr",
      domain: "sushiscan.net",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {
        listUrl: "/catalogue",
        datePattern: "MMM d, yyyy"
      }
    },
    {
      id: "THEBLANK",
      className: "TheBlank",
      title: "TheBlank",
      locale: "en",
      domain: "theblank.net",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "dd/MM/yyyy"
      }
    },
    {
      id: "THUNDERSCANS",
      className: "ThunderScans",
      title: "ThunderScans",
      locale: "ar",
      domain: "lavascans.com",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {
        listUrl: "/browse-manga",
        datePattern: "yyyy/MM/dd",
        selectMangaList: "article.legend-card",
        selectMangaListImg: "img.legend-img",
        selectMangaListTitle: "h3.legend-title a",
        selectChapter: ".ch-list-grid .ch-item"
      }
    },
    {
      id: "TOOMICSSC",
      className: "TooMicsSc",
      title: "TooMicsSc",
      locale: "zh-Hans",
      domain: "toomics.com/sc",
      family: "HotComicsParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "TOOMICSTC",
      className: "TooMicsTc",
      title: "TooMicsTc",
      locale: "zh-Hant",
      domain: "toomics.com/tc",
      family: "HotComicsParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "TOONFR",
      className: "ToonFr",
      title: "ToonFr",
      locale: "fr",
      domain: "toonfr.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        listUrl: "webtoon/",
        datePattern: "MMM d",
        tagPrefix: "webtoon-genre/"
      }
    },
    {
      id: "TOONGOD",
      className: "ToonGod",
      title: "ToonGod",
      locale: "en",
      domain: "www.toongod.org",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        listUrl: "webtoon/",
        datePattern: "d MMM yyyy",
        tagPrefix: "webtoon-genre/",
        withoutAjax: true
      }
    },
    {
      id: "TOONILY",
      className: "Toonily",
      title: "Toonily",
      locale: "en",
      domain: "toonily.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        listUrl: "webtoon/",
        datePattern: "MMMM dd, yyyy",
        tagPrefix: "webtoon-genre/"
      }
    },
    {
      id: "TOONIZY",
      className: "Toonizy",
      title: "Toonizy",
      locale: "en",
      domain: "toonizy.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        listUrl: "webtoon/",
        datePattern: "MMM d, yy"
      }
    },
    {
      id: "TOPCOMICPORNO",
      className: "TopComicPorno",
      title: "TopComicPorno",
      locale: "es",
      domain: "topcomicporno.net",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "MMM dd, yy"
      }
    },
    {
      id: "TOPTRUYEN",
      className: "TopTruyen",
      title: "TopTruyen",
      locale: "vi",
      domain: "www.toptruyentv11.com",
      family: "WpComicsParser",
      isNsfw: false,
      overrides: {
        datePattern: "dd/MM/yyyy"
      }
    },
    {
      id: "UTOON",
      className: "UToon",
      title: "UToon",
      locale: "en",
      domain: "utoon.net",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        datePattern: "dd MMM"
      }
    },
    {
      id: "VINLANDSAGA",
      className: "VinlandSaga",
      title: "VinlandSaga",
      locale: "fr",
      domain: "vinlandsaga.fr",
      family: "OneMangaParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "WALPURGISCAN",
      className: "Walpurgiscan",
      title: "WalpurgiScan",
      locale: "it",
      domain: "www.walpurgiscan.it",
      family: "MangaReaderParser",
      isNsfw: false,
      overrides: {
        datePattern: "MMM d, yyyy"
      }
    },
    {
      id: "WEBDEXSCANS",
      className: "WebDexScans",
      title: "WebDexScans",
      locale: "en",
      domain: "webdexscans.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "WEBTOONXYZ",
      className: "WebtoonXyz",
      title: "Webtoon.xyz",
      locale: "en",
      domain: "www.webtoon.xyz",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        listUrl: "read/",
        datePattern: "d MMM yyyy",
        tagPrefix: "webtoon-genre/"
      }
    },
    {
      id: "WEBTOONEMPIRE",
      className: "WebtoonEmpire",
      title: "WebtoonEmpire",
      locale: "ar",
      domain: "webtoonempire-bl.com",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        listUrl: "webtoon/",
        datePattern: "d MMMM\u060C yyyy",
        withoutAjax: true
      }
    },
    {
      id: "YAOIMANGAOKU",
      className: "YaoiMangaOku",
      title: "YaoiMangaOku",
      locale: "tr",
      domain: "yaoimangaoku.net",
      family: "MadaraParser",
      isNsfw: true,
      overrides: {
        datePattern: "d MMMM yyyy"
      }
    },
    {
      id: "YKMH",
      className: "Ykmh",
      title: "Ykmh",
      locale: "zh-Hans",
      domain: "www.ykmh.net",
      family: "SinmhParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "MANGANATO_GG",
      className: "ManganatoGg",
      title: "Manganato (.gg)",
      locale: "en",
      domain: "www.manganato.gg",
      family: "MangaBoxParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "ASURASCANS_US",
      className: "AsuraScansUs",
      title: "Asura Scans",
      locale: "en",
      domain: "asurascans.com",
      family: "MadaraParser",
      isNsfw: false,
      overrides: {
        listUrl: "browse/",
        tagPrefix: "genres/"
      }
    },
    {
      id: "MANGAFIRE_EN",
      className: "MangaFire",
      title: "MangaFire",
      locale: "en",
      domain: "mangafire.to",
      family: "MangaFireParser",
      isNsfw: false,
      overrides: {}
    },
    {
      id: "MANGAFIRE_JA",
      className: "MangaFireJa",
      title: "MangaFire (JA)",
      locale: "ja",
      domain: "mangafire.to",
      family: "MangaFireParser",
      isNsfw: false,
      overrides: {}
    }
  ];

  // src/index.js
  var FAMILIES = {
    MadaraParser,
    MangaReaderParser,
    ZeistMangaParser,
    OneMangaParser,
    HotComicsParser,
    WpComicsParser,
    PizzaReaderParser,
    KeyoappParser,
    FoolSlideParser,
    LilianaParser,
    MadthemeParser,
    ScanParser,
    IkenParser,
    MmrcmsParser,
    CupFoxParser,
    FmreaderParser,
    WeLoveMangaParser,
    AnimeBootstrapParser,
    GuyaParser,
    MangaWorldParser,
    MangAdventureParser,
    InitMangaParser,
    FuzzyDoodleParser,
    UzayMangaParser,
    ComicasoParser,
    MangoThemeParser,
    ZMangaParser,
    LikeMangaParser,
    SinmhParser,
    MangaBoxParser,
    MangaFireParser
  };
  function getParser(sourceId, context) {
    const source = sources_default.find((s) => s.id === sourceId);
    if (!source)
      return null;
    const ParserClass = FAMILIES[source.family] || BaseParser;
    const parser = new ParserClass(context, source, source.domain);
    if (source.overrides) {
      Object.assign(parser, source.overrides);
    }
    return stampIds(parser, source.id);
  }
  function stampIds(parser, token) {
    const stampChapter = (c) => {
      if (c && c.url != null)
        c.id = nyoraId(token, " chapter " + c.url);
      return c;
    };
    const stampManga = (m) => {
      if (m && m.url != null) {
        m.id = nyoraId(token, m.url);
        if (Array.isArray(m.chapters))
          m.chapters.forEach(stampChapter);
      }
      return m;
    };
    const wrap = (name, after) => {
      const fn = parser[name];
      if (typeof fn !== "function")
        return;
      parser[name] = async function(...args) {
        const result = await fn.apply(this, args);
        after(result);
        return result;
      };
    };
    wrap("getListPage", (r) => {
      if (Array.isArray(r))
        r.forEach(stampManga);
    });
    wrap("getDetails", (r) => {
      stampManga(r);
    });
    wrap("getChapters", (r) => {
      if (Array.isArray(r))
        r.forEach(stampChapter);
    });
    return parser;
  }
  function getAllSources() {
    return sources_default;
  }
  return __toCommonJS(ios_entry_exports);
})();
