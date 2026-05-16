import { useEffect, useMemo, useState } from 'react';

export default function AEDVDashboard() {
  const [categories, setCategories] = useState([]);
  const [allProducts, setAllProducts] = useState([]);
  const [recommendations, setRecommendations] = useState([]);

  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  const normalizeProducts = (raw) => {
    const map = {};

    raw.forEach((p) => {
      const id = p.id;

      if (!map[id]) {
        map[id] = {
          id,
          name: p.name,
          category: p.category,
          mercadona: null,
          carrefour: null,
          history: [],
        };
      }

      const price = Number(p.unitPrice ?? 0);
      const store = (p.ss || '').toLowerCase();

      map[id].history.push(price);

      if (store === 'mercadona') {
        map[id].mercadona = price;
      }

      if (store === 'carrefour') {
        map[id].carrefour = price;
      }
    });

    return Object.values(map);
  };

  // 🔥 LOAD DATA
  useEffect(() => {
    fetch("http://localhost:7000/api/categories")
      .then(r => r.json())
      .then(setCategories);

    fetch("http://localhost:7000/api/products")
      .then(r => r.json())
      .then((data) => setAllProducts(normalizeProducts(data)));
  }, []);

  // 🔥 RECOMMENDATIONS (PROTEGIDO)
  useEffect(() => {
    const category = selectedCategory === 'all' ? 'all' : selectedCategory;

    fetch(`http://localhost:7000/api/recommendation/${encodeURIComponent(category)}`)
      .then(r => r.json())
      .then((data) => {
        const cp = data?.cheapestProduct;

        if (!cp) {
          setRecommendations([]);
          return;
        }

        setRecommendations([
          {
            id: cp.name,
            name: cp.name,
            category: data.category,
            mercadona: cp.source === "mercadona" ? cp.price : null,
            carrefour: cp.source === "carrefour" ? cp.price : null,
          }
        ]);
      })
      .catch(() => setRecommendations([]));
  }, [selectedCategory]);

  // 🔥 FILTER PRODUCTS
  const filteredProducts = useMemo(() => {
    if (selectedCategory === 'all') return allProducts;
    return allProducts.filter((p) => p.category === selectedCategory);
  }, [selectedCategory, allProducts]);

  // 🔥 SEARCH
  const searchResults = useMemo(() => {
    if (!searchTerm) return [];
    return allProducts.filter((p) =>
      p.name.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [searchTerm, allProducts]);

  // 🔥 SAFE PRICE DIFF
  const priceDiff = (a, b) => {
    if (a == null || b == null) return '0.0';
    const diff = ((b - a) / b) * 100;
    return diff.toFixed(1);
  };

  // 🔥 CATEGORY STATS SAFE
  const categoryStats = useMemo(() => {
    return categories.map((cat) => {
      const items = allProducts.filter((p) => p.category === cat);

      const total = items.reduce((acc, p) => acc + (p.mercadona || 0), 0);
      const avg = items.length ? total / items.length : 0;

      return {
        name: cat,
        count: items.length,
        avg: avg.toFixed(2),
      };
    });
  }, [categories, allProducts]);

  // 🔥 AUTO SELECT PRODUCT
  useEffect(() => {
    if (filteredProducts.length > 0) {
      setSelectedProduct(filteredProducts[0]);
    } else {
      setSelectedProduct(null);
    }
  }, [filteredProducts]);

  const history = selectedProduct?.history ?? [];
  const maxHistory = history.length ? Math.max(...history) : 1;

  return (
    <div className="min-h-screen bg-slate-950 text-white p-6 lg:p-10">
      <div className="max-w-5xl mx-auto space-y-10">

        {/* HEADER */}
        <div>
          <h1 className="text-5xl xl:text-6xl font-black tracking-tight">
            AEDV Market Analytics
          </h1>
          <p className="text-slate-400 mt-5 text-lg">
            Compare supermarket prices, track evolution and detect best offers.
          </p>
        </div>

        {/* KPI */}
        <div className="grid grid-cols-2 xl:grid-cols-4 gap-6">
          <KPI title="Products Available" value={allProducts.length} />
          <KPI title="Price Updates" value="3,584" />
          <KPI title="Categories" value={categories.length} />
          <KPI title="Offers Detected" value={recommendations.length} />
        </div>

        {/* 🔥 TRENDING FIXED */}
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-8">
          <h2 className="text-2xl font-bold mb-6">Recommended Products</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {(recommendations || []).slice(0, 5).map((p) => {
              const mercadona = p?.mercadona ?? null;
              const carrefour = p?.carrefour ?? null;

              const minPrice =
                mercadona != null && carrefour != null
                  ? Math.min(mercadona, carrefour)
                  : mercadona ?? carrefour ?? 0;

              return (
                <div key={p.id} className="bg-slate-800 p-5 rounded-2xl">
                  <div className="flex justify-between">
                    <h3 className="font-bold">{p.name}</h3>

                    <span className="text-emerald-400 text-sm font-semibold">
                      €{minPrice.toFixed(2)}
                    </span>
                  </div>

                  <p className="text-slate-400 text-sm mt-1 capitalize">
                    {p.category}
                  </p>

                  <div className="text-sm mt-3 text-slate-300">
                    Mercadona: €{mercadona ?? "-"} · Carrefour: €{carrefour ?? "-"}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* CATEGORIES (SIN CAMBIOS DE UI) */}
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-8">
          <h2 className="text-2xl font-bold mb-6">Product Categories</h2>

          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="bg-slate-800 border border-slate-700 rounded-2xl px-5 py-4 text-white"
          >
            <option value="all">All categories</option>
            {categories.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>

          {selectedCategory === 'all' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
              {categoryStats.map((cat) => (
                <button
                  key={cat.name}
                  onClick={() => setSelectedCategory(cat.name)}
                  className="w-full text-left bg-slate-800 border border-slate-700 rounded-2xl p-5 hover:border-slate-500"
                >
                  <div className="flex justify-between items-center">
                    <h3 className="font-bold capitalize">{cat.name}</h3>
                    <span className="text-cyan-400 font-semibold text-sm">
                      {cat.count} items
                    </span>
                  </div>
                  <p className="text-slate-400 text-sm mt-2">
                    Avg price: €{cat.avg}
                  </p>
                </button>
              ))}
            </div>
          )}

          {selectedCategory !== 'all' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
              {filteredProducts.map((p) => (
                <div key={p.id} className="bg-slate-800 p-5 rounded-2xl">
                  <div className="flex justify-between">
                    <h3 className="font-bold">{p.name}</h3>
                    <span className="text-emerald-400 text-sm font-semibold">
                      {priceDiff(p.mercadona, p.carrefour)}% cheaper
                    </span>
                  </div>

                  <p className="text-slate-400 text-sm mt-1 capitalize">
                    {p.category}
                  </p>

                  <div className="text-sm mt-3 text-slate-300">
                    Mercadona: €{p.mercadona ?? 0} · Carrefour: €{p.carrefour ?? 0}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* PRODUCT DETAIL (UNCHANGED LOGIC) */}
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-8">

          <p className="text-cyan-400 uppercase text-sm mb-3">
            Product Detail
          </p>

          <input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search product..."
            className="w-full bg-slate-800 border border-slate-700 rounded-2xl px-5 py-4 mb-4"
          />

          {searchTerm && (
            <div className="mb-6 space-y-2">
              {searchResults.map((p) => (
                <button
                  key={p.id}
                  onClick={() => {
                    setSelectedProduct(p);
                    setSearchTerm('');
                  }}
                  className="w-full text-left bg-slate-800 p-4 rounded-2xl"
                >
                  <div className="font-bold">{p.name}</div>
                  <div className="text-slate-400 text-sm">{p.category}</div>
                </button>
              ))}
            </div>
          )}

          {selectedProduct && (
            <>
              <h2 className="text-4xl font-black">{selectedProduct.name}</h2>
              <p className="text-slate-400 mt-2 capitalize">
                {selectedProduct.category}
              </p>

              <div className="grid grid-cols-2 gap-6 mt-6">
                <PriceCard label="Mercadona" value={selectedProduct.mercadona} />
                <PriceCard label="Carrefour" value={selectedProduct.carrefour} />
              </div>

              <div className="mt-8">
                <h3 className="text-xl font-bold mb-4">
                  Historical Evolution
                </h3>

                <div className="relative flex items-end gap-2 h-52 bg-slate-800 p-6 rounded-3xl overflow-hidden">
                  <div className="absolute bottom-6 left-6 right-6 h-px bg-slate-600/50" />

                  {history.map((h, i) => (
                    <div key={i} className="flex-1 flex flex-col justify-end">
                      <div
                        className="bg-gradient-to-t from-cyan-500 to-emerald-400 rounded-t-xl"
                        style={{
                          height: `${(h / maxHistory) * 100}%`,
                        }}
                      />
                      <span className="text-xs text-slate-400 mt-2 text-center">
                        D{i + 1}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>

      </div>
    </div>
  );


/* COMPONENTS unchanged */
function KPI({ title, value }) {
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-3xl p-7">
      <p className="text-slate-400 text-sm uppercase">{title}</p>
      <h2 className="text-4xl font-black mt-2">{value}</h2>
    </div>
  );
}

function PriceCard({ label, value }) {
  return (
    <div className="bg-slate-800 rounded-3xl p-6 border border-slate-700">
      <p className="text-slate-400 text-sm">{label}</p>
      <p className="text-3xl font-black mt-2">€{value}</p>
    </div>
  );
}
}