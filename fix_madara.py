import os
import re

file_path = "/Users/hasanraza/Desktop/nyora-ota-parsers/src/madara.js"
with open(file_path, "r") as f:
    content = f.read()

# 1. Update asuraApiBase and asuraCdnBase
content = re.sub(r'asuraApiBase\(\) \{.*?\}', 'asuraApiBase() {\n        return "https://api.asurascans.com";\n    }', content, flags=re.DOTALL)
content = re.sub(r'asuraCdnBase\(\) \{.*?\}', 'asuraCdnBase() {\n        return "https://cdn.asurascans.com";\n    }', content, flags=re.DOTALL)

# 2. Optimized and Robust getAsuraDetails
DETAILS = """    async getAsuraDetails(manga) {
        let key = this.asuraSeriesKey(manga.url);
        const apiBase = this.asuraApiBase();
        
        // Helper to fetch from API with Referer
        const fetchSeries = async (k) => {
            if (!k) return null;
            try {
                const text = await this.context.httpGet(apiBase + "/api/series/" + k + "?nyoraTry=" + Date.now(), this);
                const res = JSON.parse(text);
                const s = res.series || res.data?.series || res.data || res;
                return (s && s.title) ? s : null;
            } catch { return null; }
        };

        let series = await fetchSeries(key);
        
        // If API fails, try search resolution
        if (!series && manga.title) {
             try {
                const searchTerm = manga.title.replace(/['’]/g, "").replace(/\\s+/g, " ").trim();
                const searchUrl = "https://asurascans.com/browse?search=" + encodeURIComponent(searchTerm);
                const searchHtml = await this.context.httpGet(searchUrl, this);
                const searchDoc = this.context.parseHTML(searchHtml);
                const links = Array.from(searchDoc.querySelectorAll('a[href*="/series/"], a[href*="/comics/"]'));
                const normalize = (t) => (t || "").toLowerCase().replace(/[^a-z0-9]/g, "");
                const targetTitle = normalize(manga.title);
                const foundA = links.find(a => normalize(a.textContent) === targetTitle) || links[0];
                if (foundA) {
                    const newRel = this.toRelativeUrl(foundA.getAttribute("href")).replace(/\\/$/, "");
                    const newKey = this.asuraSeriesKey(newRel);
                    if (newKey && newKey !== key) {
                        series = await fetchSeries(newKey);
                        if (series) key = newKey;
                    }
                }
             } catch (e) {}
        }

        if (!series) series = {};

        const parseDate = (d) => {
            if (!d) return null;
            try {
                const date = new Date(d);
                return isNaN(date.getTime()) ? null : date.toISOString();
            } catch { return null; }
        };

        let chapterRows = [];
        try {
            const text = await this.context.httpGet(apiBase + "/api/series/" + key + "/chapters?nyoraTry=" + Date.now(), this);
            const chaptersRes = JSON.parse(text);
            chapterRows = Array.isArray(chaptersRes.data) ? chaptersRes.data : [];
        } catch {
            chapterRows = [];
        }
        
        const publicUrl = "https://asurascans.com/comics/" + key;
        const chapters = chapterRows.map((row) => new MangaChapter({
            id: publicUrl + "/chapter/" + row.number,
            url: publicUrl + "/chapter/" + row.number,
            title: row.title || ("Chapter " + row.number),
            number: Number(row.number) || 0,
            uploadDate: parseDate(row.published_at),
            source: this.source
        }));

        // FALLBACK: If API still has no chapters, try parsing them from the HTML page directly
        if (chapters.length === 0) {
            try {
                const html = await this.context.httpGet("https://asurascans.com/comics/" + key, this);
                const doc = this.context.parseHTML(html);
                const links = Array.from(doc.querySelectorAll('a[href*="/chapter/"]'));
                chapters = links.map((a, i) => {
                    const href = a.getAttribute("href");
                    const relHref = this.toRelativeUrl(href).replace(/\\/$/, "");
                    const titleText = a.textContent.trim();
                    const numMatch = titleText.match(/Chapter\\s+([\\d.]+)/i);
                    return new MangaChapter({
                        id: relHref,
                        url: relHref,
                        title: titleText,
                        number: numMatch ? parseFloat(numMatch[1]) : (links.length - i),
                        source: this.source
                    });
                }).filter(c => c.url.includes(key));
            } catch (e) {}
        }

        return new Manga({
            ...manga,
            id: publicUrl,
            url: publicUrl,
            publicUrl: publicUrl,
            title: series.title || manga.title,
            description: series.description || "",
            authors: [series.author, series.artist].filter(Boolean),
            tags: (series.genres || []).map((genre) => ({ title: genre.name, key: genre.slug || genre.name })),
            state: String(series.status || "").toLowerCase() === "dropped" ? MangaState.ABANDONED : MangaState.ONGOING,
            contentRating: ContentRating.SAFE,
            source: this.source,
            chapters
        });
    }"""

content = re.sub(r'async getAsuraDetails\(manga\) \{.*?\n    \}', DETAILS, content, flags=re.DOTALL)

# 3. Ensure asuraPages also passes this
content = content.replace('this.context.httpGet(`${this.asuraApiBase()}/api/series/${key}/chapters/${number}?nyoraTry=${Date.now()}`)',
                         'this.context.httpGet(`${this.asuraApiBase()}/api/series/${key}/chapters/${number}?nyoraTry=${Date.now()}`, this)')

with open(file_path, "w") as f:
    f.write(content)
