import re
import os

repo_path = "/Users/hasanraza/Desktop/nyora-ota-parsers"

# 1. Update madara.js
madara_path = os.path.join(repo_path, "src/madara.js")
with open(madara_path, "r") as f:
    content = f.read()

# Unified API and CDN base
content = re.sub(r'asuraApiBase\(\) \{.*?\}', 
                 'asuraApiBase() {\n        return "https://api.asurascans.com";\n    }', 
                 content, flags=re.DOTALL)
content = re.sub(r'asuraCdnBase\(\) \{.*?\}', 
                 'asuraCdnBase() {\n        return "https://cdn.asurascans.com";\n    }', 
                 content, flags=re.DOTALL)

# Flexible asuraSeriesKey
content = re.sub(r'asuraSeriesKey\(url\) \{.*?\}', 
                 'asuraSeriesKey(url) {\n        const rel = this.toRelativeUrl(url || "");\n        const match = rel.match(/\\/(series|comics|manga)\\/([^/?#]+)/);\n        return match ? match[2] : "";\n    }', 
                 content, flags=re.DOTALL)

# Robust getAsuraDetails with resolution
new_details = """    async getAsuraDetails(manga) {
        let key = this.asuraSeriesKey(manga.url);
        if (!key) throw new Error("Missing Asura series key");
        
        const apiBase = this.asuraApiBase();
        const fetchSeries = async (k) => {
            try {
                const text = await this.context.httpGet(`${apiBase}/api/series/${k}?nyoraTry=${Date.now()}`);
                const res = JSON.parse(text);
                return res.series || res.data || res;
            } catch { return null; }
        };

        let series = await fetchSeries(key);
        
        if (!series || !series.title) {
             try {
                const searchUrl = `https://${this.domain}/browse?search=${encodeURIComponent(manga.title)}`;
                const searchHtml = await this.context.httpGet(searchUrl, this);
                const searchDoc = this.context.parseHTML(searchHtml);
                const links = Array.from(searchDoc.querySelectorAll('a[href*="/series/"], a[href*="/comics/"]'));
                const foundA = links.find(a => a.textContent.trim().toLowerCase() === manga.title.toLowerCase()) || links[0];
                
                if (foundA) {
                    const newRel = this.toRelativeUrl(foundA.getAttribute("href")).replace(/\\/$/, "");
                    key = this.asuraSeriesKey(newRel);
                    if (key) series = await fetchSeries(key);
                }
             } catch {}
        }

        if (!series || !series.title) series = {};

        let chapterRows = [];
        try {
            const chaptersRes = await this.getJson(`${apiBase}/api/series/${key}/chapters?nyoraTry=${Date.now()}`);
            chapterRows = Array.isArray(chaptersRes.data) ? chaptersRes.data : [];
        } catch {
            chapterRows = [];
        }
        
        const publicUrl = series.public_url || `/series/${key}`;
        const chapters = chapterRows.map((row) => new MangaChapter({
            id: `${publicUrl}/chapter/${row.number}`,
            url: `${publicUrl}/chapter/${row.number}`,
            title: row.title || `Chapter ${row.number}`,
            number: Number(row.number) || 0,
            uploadDate: row.published_at ? new Date(row.published_at).toISOString() : null,
            source: this.source
        }));

        return new Manga({
            ...manga,
            id: publicUrl,
            url: publicUrl,
            publicUrl: this.toAbsoluteUrl(publicUrl),
            title: series.title || manga.title,
            altTitles: series.alt_titles || [],
            description: series.description || "",
            coverUrl: series.cover || manga.coverUrl || "",
            largeCoverUrl: series.cover || manga.largeCoverUrl || manga.coverUrl || "",
            rating: Number(series.rating) || 0,
            authors: [series.author, series.artist].filter(Boolean),
            tags: (series.genres || []).map((genre) => ({ title: genre.name, key: genre.slug || genre.name })),
            state: String(series.status || "").toLowerCase() === "dropped" ? MangaState.ABANDONED : MangaState.ONGOING,
            contentRating: ContentRating.SAFE,
            source: this.source,
            chapters
        });
    }"""
content = re.sub(r'async getAsuraDetails\(manga\) \{.*?\n    \}', new_details, content, flags=re.DOTALL)

with open(madara_path, "w") as f:
    f.write(content)

# 2. Update sources.json
sources_path = os.path.join(repo_path, "src/sources.json")
with open(sources_path, "r") as f:
    s_content = f.read()

s_content = s_content.replace('"locale": "pt"', '"locale": "pt-BR"')
s_content = re.sub(r'("id": "TOOMICSSC".*?"locale": ")zh"', r'\1zh-Hans"', s_content, flags=re.DOTALL)
s_content = re.sub(r'("id": "TOOMICSTC".*?"locale": ")zh"', r'\1zh-Hant"', s_content, flags=re.DOTALL)
s_content = re.sub(r'("id": "YKMH".*?"locale": ")zh"', r'\1zh-Hans"', s_content, flags=re.DOTALL)
s_content = re.sub(r'("id": "ASURASCANS_US".*?"listUrl": ")comics/"', r'\1series/"', s_content, flags=re.DOTALL)
s_content = re.sub(r'("id": "ASURASCANS_US".*?"tagPrefix": ")read-en-us-genre/"', r'\1genres/"', s_content, flags=re.DOTALL)

with open(sources_path, "w") as f:
    f.write(s_content)

