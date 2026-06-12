content = r"""import { BaseParser, Manga, MangaChapter, MangaPage, MangaState, SortOrder, ContentRating } from './base.js';

const DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

export class MadaraParser extends BaseParser {
    constructor(context, source, domain, pageSize = 12) {
        super(context, source, domain, pageSize);
        this.withoutAjax = false;
        this.tagPrefix = "manga-genre/";
        this.datePattern = "MMMM d, yyyy";
        this.stylePage = "?style=list";
        this.postReq = false;

        this.ongoing = new Set([
            "مستمرة", "en curso", "ongoing", "on going", "OnGoing", "ativo", "en cours",
            "en cours \uD83D\uDFE2", "en cours de publication", "activo", "đang tiến hành",
            "em lançamento", "онгоінг", "publishing", "devam ediyor", "em andamento",
            "in corso", "güncel", "berjalan", "продолжается", "updating", "lançando",
            "in arrivo", "emision", "en emision", "مستمر", "curso", "en marcha",
            "publicandose", "publicando", "连载中"
        ]);

        this.finished = new Set([
            "completed", "complete", "completo", "complété", "fini", "achevé", "terminé",
            "terminé ⚫", "tamamlandı", "đã hoàn thành", "hoàn thành", "مكتملة",
            "завершено", "завершен", "finished", "finalizado", "completata", "one-shot",
            "bitti", "tamat", "completado", "concluído", "concluido", "已完结", "bitmiş",
            "end", "منتهية"
        ]);

        this.abandoned = new Set([
            "canceled", "cancelled", "cancelado", "cancellato", "cancelados", "dropped",
            "discontinued", "abandonné"
        ]);

        this.paused = new Set([
            "hiatus", "on hold", "pausado", "en espera", "en pause", "en attente"
        ]);

        this.upcoming = new Set([
            "upcoming", "لم تُنشَر بعد", "prochainement", "à venir"
        ]);
    }

    queryAll(doc, selectors) {
        for (const selector of selectors.filter(Boolean)) {
            try {
                const elements = Array.from(doc.querySelectorAll(selector));
                if (elements.length) return elements;
            } catch {
            }
        }
        return [];
    }

    imageSrc(img) {
        const url = img ? (img.getAttribute("data-src") || img.getAttribute("data-lazy-src") || img.getAttribute("src") || "") : "";
        if (!url || url.startsWith("data:") || url.startsWith("blob:")) return url || "";
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
        if (filter.query) url += `&search=${encodeURIComponent(filter.query)}`;
        const html = await this.context.httpGet(url, { "User-Agent": DESKTOP_UA }, this);
        const doc = this.context.parseHTML(html);
        const seen = new Set();
        const entries = [];
        for (const a of Array.from(doc.querySelectorAll('a[href*="/series/"], a[href*="/comics/"], a[href*="/manga/"]'))) {
            const href = a.getAttribute("href") || "";
            if (!href || href.includes("/chapter/")) continue;
            const relHref = this.toRelativeUrl(href).replace(/\/$/, "");
            if (seen.has(relHref)) continue;
            const img = a.querySelector("img");
            const title = (img && img.getAttribute("alt") || a.textContent || "").trim();
            if (!title || title.length > 120) continue;
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
        let description = doc.querySelector('meta[name="description"]')?.getAttribute("content") || 
                          doc.querySelector('meta[property="og:description"]')?.getAttribute("content") || "";
        
        let chapters = Array.from(doc.querySelectorAll('a[href*="/chapter/"]')).map((a, i, all) => {
            const href = a.getAttribute("href");
            const relHref = this.toRelativeUrl(href).replace(/\/$/, "");
            const titleText = a.textContent.trim().replace(/\s+/g, " ");
            const numMatch = titleText.match(/Chapter\s+([\d.]+)/i);
            return new MangaChapter({
                id: relHref,
                url: relHref,
                title: titleText,
                number: numMatch ? parseFloat(numMatch[1]) : (all.length - i),
                source: this.source
            });
        }).filter(c => c.url.includes(key || ""));

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
                    chapters = rows.map(row => new MangaChapter({
                        id: `${canonical}/chapter/${row.number}`,
                        url: `${canonical}/chapter/${row.number}`,
                        title: row.title || `Chapter ${row.number}`,
                        number: Number(row.number) || 0,
                        source: this.source
                    }));
                }
            } catch (e) {}
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

        let imageUrls = Array.from(doc.querySelectorAll("img[data-page-index], .reading-content img, .page-break img"))
            .map((img) => this.imageSrc(img))
            .filter(src => src && src.includes("asura-images"));
        
        if (!imageUrls.length) {
            imageUrls = Array.from(html.matchAll(/https:\/\/cdn\.asurascans\.com\/asura-images\/chapters\/[^"'<>\s)]+/g))
                .map((match) => match[0]);
        }

        if (imageUrls.length) {
            return imageUrls.map((url, i) => new MangaPage({
                id: url,
                url,
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
                })).filter(p => p.url);
            } catch (e) {}
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
            ".chapters li",
        ]).reverse();

        return elements.map((el, i) => {
            const a = el.querySelector("a");
            if (!a) return null;
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
            if (pages > 1) url += `/page/${pages}`;
            url += `/?s=${encodeURIComponent(filter.query || "")}&post_type=wp-manga`;

            let orderStr = "";
            switch (order) {
                case SortOrder.POPULARITY: orderStr = "views"; break;
                case SortOrder.UPDATED: orderStr = "latest"; break;
                case SortOrder.NEWEST: orderStr = "new-manga"; break;
                case SortOrder.ALPHABETICAL: orderStr = "alphabet"; break;
                case SortOrder.RATING: orderStr = "rating"; break;
            }
            if (orderStr) url += `&m_orderby=${orderStr}`;

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
            if (!a) continue;
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
            chapters: chapters
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
                        'Content-Type': 'application/x-www-form-urlencoded'
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
        return Array.from(images).map(img => {
            const imageUrl = this.imageSrc(img);
            return new MangaPage({
                id: imageUrl,
                url: imageUrl,
                source: this.source
            });
        });
    }
}
"""

with open("/Users/hasanraza/Desktop/nyora-ota-parsers/src/madara.js", "w") as f:
    f.write(content)
