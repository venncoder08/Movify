const PlaceModel = require("../models/placeModel");
const geoapifyRepository = require("../repositories/geoapifyRepository");
const env = require("../config/env");
const AppError = require("../utils/httpError");

const COORDINATE_PATTERN = /(-?\d{1,2}\.\d{3,})\s*,\s*(-?\d{1,3}\.\d{3,})/;
const DEFAULT_RECOMMENDATION_RADIUS_METERS = 5000;
const DEFAULT_RECOMMENDATION_LIMIT = 10;
const MAX_RECOMMENDATION_LIMIT = 20;
const BROAD_FALLBACK_CATEGORIES = [
  "service",
  "commercial",
  "catering",
  "healthcare",
  "leisure",
  "education",
  "tourism"
];

const PLACE_INTENTS = [
  {
    key: "dentist",
    label: "Nha khoa",
    categories: ["healthcare.dentist"],
    keywords: ["nha khoa", "phong kham rang", "rang ham mat", "tram rang", "chinh nha", "dentist", "dental"]
  },
  {
    key: "hospital",
    label: "Y tế",
    categories: ["healthcare.hospital", "healthcare.clinic_or_praxis", "healthcare.pharmacy"],
    keywords: ["benh vien", "phong kham", "bac si", "y te", "cap cuu", "nha thuoc", "hospital", "clinic", "pharmacy"]
  },
  {
    key: "post",
    label: "Bưu chính",
    categories: ["service.post.office", "service.post", "service.post.parcel_locker"],
    keywords: ["buu dien", "buu cuc", "gui hang", "chuyen phat", "postal", "post office", "parcel"]
  },
  {
    key: "park",
    label: "Công viên",
    categories: ["leisure.park", "leisure.park.garden", "leisure.playground", "national_park", "natural.protected_area"],
    keywords: ["cong vien", "vuon hoa", "san choi", "khu vui choi", "park", "garden", "playground"]
  },
  {
    key: "food",
    label: "Ăn uống",
    categories: ["catering.restaurant", "catering.cafe", "catering.fast_food", "catering.restaurant.vietnamese"],
    keywords: ["quan an", "nha hang", "cafe", "ca phe", "tra sua", "an uong", "restaurant", "coffee"]
  },
  {
    key: "internet_cafe",
    label: "Quán net",
    categories: ["entertainment.amusement_arcade", "entertainment", "commercial"],
    conditions: ["internet_access"],
    keywords: ["quan net", "tiem net", "phong net", "net", "cyber", "cyber game", "game net", "internet cafe", "gaming", "pc gaming"],
    placeKeywords: ["net", "cyber", "gaming", "internet", "esport", "e-sport"],
    nameSearchTerms: ["net", "cyber", "game"],
    geocodeSearchTerms: ["internet cafe", "cyber game", "quan internet"],
    strongCategories: ["entertainment.amusement_arcade"],
    requirePlaceKeywordOrStrongCategory: true
  },
  {
    key: "shopping",
    label: "Mua sắm",
    categories: ["commercial.supermarket", "commercial.convenience", "commercial.shopping_mall", "commercial.department_store"],
    keywords: ["sieu thi", "tap hoa", "cua hang", "trung tam thuong mai", "mua sam", "supermarket", "mall", "market"]
  },
  {
    key: "bank",
    label: "Tài chính",
    categories: ["service.financial.bank", "service.financial.atm", "service.financial.money_transfer"],
    keywords: ["ngan hang", "atm", "rut tien", "chuyen tien", "bank"]
  },
  {
    key: "education",
    label: "Giáo dục",
    categories: ["education.school", "education.university", "education.college", "education.library"],
    keywords: ["truong hoc", "dai hoc", "thu vien", "giao duc", "school", "university", "library"],
    placeKeywords: ["dai hoc", "truong dai hoc", "dh"],
    strongCategories: ["education.university"]
  },
  {
    key: "kindergarten",
    label: "Mẫu giáo",
    categories: ["childcare.kindergarten", "childcare"],
    keywords: ["mau giao", "mam non", "nha tre", "giu tre", "truong mam non", "kindergarten", "preschool", "childcare"]
  },
  {
    key: "police",
    label: "Công an",
    categories: ["service.police"],
    keywords: ["cong an", "canh sat", "police"]
  },
  {
    key: "fuel",
    label: "Trạm xăng",
    categories: ["service.vehicle.fuel", "service.vehicle.charging_station"],
    keywords: ["tram xang", "cay xang", "sac xe", "fuel", "gas station", "charging"]
  },
  {
    key: "hotel",
    label: "Lưu trú",
    categories: ["accommodation.hotel", "accommodation.hostel", "accommodation.guest_house"],
    keywords: ["khach san", "nha nghi", "homestay", "hotel", "hostel"]
  }
];
const autocompleteCache = new Map();

async function autocomplete(query) {
  const normalizedQuery = String(query || "").trim();
  if (!normalizedQuery) {
    return { suggestions: [] };
  }

  const cachedPayload = getCachedAutocomplete(normalizedQuery);
  if (cachedPayload) {
    return cachedPayload;
  }

  const payload = await geoapifyRepository.fetchAutocompleteResults(normalizedQuery);
  const suggestions = [];
  const seen = new Set();

  for (const result of payload.results || []) {
    const label = result.formatted?.trim();
    if (!label || seen.has(label)) {
      continue;
    }
    seen.add(label);
    suggestions.push(label);
  }

  const response = { suggestions };
  setCachedAutocomplete(normalizedQuery, response);
  return response;
}

async function geocode(query) {
  const place = await findPlaceByQuery(query, {
    missingMessage: "Thiếu tham số địa điểm",
    notFoundMessage: "Không tìm thấy địa điểm"
  });

  return { location: place };
}

async function recommendNearby({ query, lat, lng, radiusMeters, limit }) {
  const normalizedQuery = String(query || "").trim();
  if (!normalizedQuery) {
    throw new AppError("Thiếu nội dung cần tìm", 400);
  }

  const originLat = Number(lat);
  const originLng = Number(lng);
  if (!isValidCoordinate(originLat, originLng)) {
    throw new AppError("Vị trí hiện tại không hợp lệ", 400);
  }

  const searchRadiusMeters = normalizeRadius(radiusMeters);
  const searchLimit = normalizeLimit(limit);
  const matchedIntents = resolvePlaceIntents(normalizedQuery);
  const categories = getCategoriesForIntents(matchedIntents);
  const conditions = getConditionsForIntents(matchedIntents);
  const searchRequests = buildNearbySearchRequests({
    categories,
    conditions,
    query: normalizedQuery,
    intents: matchedIntents,
    lat: originLat,
    lng: originLng,
    radiusMeters: searchRadiusMeters,
    limit: Math.min(searchLimit * 2, MAX_RECOMMENDATION_LIMIT)
  });
  const payloads = await Promise.all(
    searchRequests.map((request) => geoapifyRepository.fetchNearbyPlaces(request))
  );
  const geocodePayloads = await Promise.all(
    buildNearbyGeocodeSearchTerms(normalizedQuery, matchedIntents)
      .map((term) => geoapifyRepository.fetchNearbyGeocodeResults({
        query: term,
        lat: originLat,
        lng: originLng,
        radiusMeters: searchRadiusMeters,
        limit: Math.min(searchLimit * 2, MAX_RECOMMENDATION_LIMIT)
      }))
  );
  const features = [
    ...payloads.flatMap((payload) => payload.features || []),
    ...geocodePayloads.flatMap((payload) => convertGeocodeResultsToFeatures(payload.results || [], originLat, originLng))
  ];

  const recommendations = rankRecommendations({
    features,
    query: normalizedQuery,
    intents: matchedIntents,
    originLat,
    originLng,
    limit: searchLimit
  });

  return {
    data: {
      query: normalizedQuery,
      radiusMeters: searchRadiusMeters,
      origin: {
        lat: originLat,
        lng: originLng
      },
      matchedIntents: matchedIntents.map((intent) => ({
        key: intent.key,
        label: intent.label,
        score: round(intent.score, 2)
      })),
      categories,
      conditions,
      recommendations
    }
  };
}

async function findPlaceByQuery(query, { missingMessage, notFoundMessage }) {
  if (!query) {
    throw new AppError(missingMessage, 400);
  }

  const embeddedCoordinates = parseEmbeddedCoordinates(query);
  if (embeddedCoordinates) {
    return new PlaceModel({
      label: query,
      lat: embeddedCoordinates.lat,
      lng: embeddedCoordinates.lng
    });
  }

  const payload = await geoapifyRepository.fetchGeocodeResults(query, 1);
  const firstResult = payload.results?.[0];
  if (!firstResult) {
    throw new AppError(notFoundMessage, 404);
  }

  return new PlaceModel({
    label: firstResult.formatted || query,
    lat: firstResult.lat,
    lng: firstResult.lon
  });
}

function parseEmbeddedCoordinates(query) {
  const match = query.match(COORDINATE_PATTERN);
  if (!match) {
    return null;
  }

  const lat = Number(match[1]);
  const lng = Number(match[2]);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return null;
  }

  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
    return null;
  }

  return { lat, lng };
}

function resolvePlaceIntents(query) {
  const normalizedQuery = normalizeText(query);
  const queryTokens = tokenize(normalizedQuery);
  const intents = [];

  for (const intent of PLACE_INTENTS) {
    const score = calculateIntentScore(normalizedQuery, queryTokens, intent);
    if (score >= 0.35) {
      intents.push({
        ...intent,
        score
      });
    }
  }

  return intents
    .sort((a, b) => b.score - a.score)
    .filter((intent, _index, sortedIntents) => {
      const topScore = sortedIntents[0]?.score || 0;
      return intent.score >= Math.max(0.6, topScore * 0.7);
    })
    .slice(0, 3);
}

function calculateIntentScore(normalizedQuery, queryTokens, intent) {
  let bestScore = 0;
  for (const keyword of intent.keywords) {
    const normalizedKeyword = normalizeText(keyword);
    if (normalizedQuery === normalizedKeyword) {
      bestScore = Math.max(bestScore, 1);
      continue;
    }

    if (normalizedQuery.includes(normalizedKeyword) || normalizedKeyword.includes(normalizedQuery)) {
      bestScore = Math.max(bestScore, 0.9);
      continue;
    }

    const keywordTokens = tokenize(normalizedKeyword);
    const sharedTokenCount = keywordTokens.filter((token) => queryTokens.includes(token)).length;
    if (sharedTokenCount > 0) {
      bestScore = Math.max(bestScore, sharedTokenCount / Math.max(keywordTokens.length, queryTokens.length));
    }
  }

  return bestScore;
}

function getCategoriesForIntents(intents) {
  const categories = [];
  for (const intent of intents) {
    for (const category of intent.categories) {
      if (!categories.includes(category)) {
        categories.push(category);
      }
    }
  }

  return categories.length > 0 ? categories : BROAD_FALLBACK_CATEGORIES;
}

function getConditionsForIntents(intents) {
  const conditions = [];
  for (const intent of intents) {
    for (const condition of intent.conditions || []) {
      if (!conditions.includes(condition)) {
        conditions.push(condition);
      }
    }
  }

  return conditions;
}

function buildNearbySearchRequests({ categories, conditions, query, intents, lat, lng, radiusMeters, limit }) {
  const baseRequest = {
    categories,
    conditions,
    lat,
    lng,
    radiusMeters,
    limit
  };

  if (!intents.length) {
    return [
      {
        ...baseRequest,
        name: query
      }
    ];
  }

  const requests = [baseRequest];
  for (const term of intents[0].nameSearchTerms || []) {
    requests.push({
      ...baseRequest,
      name: term
    });
  }

  return requests;
}

function buildNearbyGeocodeSearchTerms(query, intents) {
  const terms = [];
  for (const term of intents[0]?.geocodeSearchTerms || []) {
    if (!terms.includes(term)) {
      terms.push(term);
    }
  }

  if (!intents.length && query && !terms.includes(query)) {
    terms.push(query);
  }

  return terms;
}

function convertGeocodeResultsToFeatures(results, originLat, originLng) {
  return results
    .map((result) => {
      const lat = Number(result.lat);
      const lng = Number(result.lon);
      if (!isValidCoordinate(lat, lng)) {
        return null;
      }

      return {
        properties: {
          place_id: result.place_id || null,
          name: result.name || result.address_line1 || result.formatted,
          formatted: result.formatted,
          lat,
          lon: lng,
          distance: calculateDistanceMeters(originLat, originLng, lat, lng),
          categories: parseResultCategories(result.category)
        }
      };
    })
    .filter(Boolean);
}

function parseResultCategories(category) {
  if (!category) {
    return [];
  }

  return String(category)
    .split(";")
    .map((item) => item.trim())
    .filter(Boolean);
}

function rankRecommendations({ features, query, intents, originLat, originLng, limit }) {
  const seen = new Set();
  return features
    .map((feature) => serializeRecommendation(feature, query, intents, originLat, originLng))
    .filter(Boolean)
    .filter((recommendation) => {
      const normalizedName = normalizeText(recommendation.name);
      const coordinateKey = `${round(recommendation.lat, 6)},${round(recommendation.lng, 6)}`;
      const placeIdKey = recommendation.placeId ? `id:${recommendation.placeId}` : null;
      const nameCoordinateKey = `name:${normalizedName}|${coordinateKey}`;
      if ((placeIdKey && seen.has(placeIdKey)) || seen.has(nameCoordinateKey)) {
        return false;
      }
      if (placeIdKey) {
        seen.add(placeIdKey);
      }
      seen.add(nameCoordinateKey);
      return true;
    })
    .sort((a, b) => b.recommendationScore - a.recommendationScore)
    .slice(0, limit);
}

function serializeRecommendation(feature, query, intents, originLat, originLng) {
  const properties = feature.properties || {};
  const lon = Number(properties.lon ?? feature.geometry?.coordinates?.[0]);
  const lat = Number(properties.lat ?? feature.geometry?.coordinates?.[1]);
  if (!isValidCoordinate(lat, lon)) {
    return null;
  }

  const name = properties.name || properties.address_line1 || properties.formatted || "Địa điểm phù hợp";
  const address = properties.formatted || [properties.address_line1, properties.address_line2]
    .filter(Boolean)
    .join(", ");
  const categories = Array.isArray(properties.categories) ? properties.categories : [];
  const visiblePlaceText = [name, address].join(" ");
  const searchablePlaceText = [visiblePlaceText, categories.join(" ")].join(" ");
  if (!passesIntentSpecificFilter(categories, visiblePlaceText, intents)) {
    return null;
  }

  const categoryMatchScore = calculateCategoryMatchScore(categories, intents);
  const textScore = calculateTextSimilarity(query, searchablePlaceText);
  const intentTextScore = calculateIntentPlaceTextScore(visiblePlaceText, intents);
  const semanticScore = Math.max(categoryMatchScore, textScore, intentTextScore);
  const distanceMeters = Number.isFinite(Number(properties.distance))
    ? Number(properties.distance)
    : calculateDistanceMeters(originLat, originLng, lat, lon);
  const distanceScore = Math.max(0, 1 - (distanceMeters / DEFAULT_RECOMMENDATION_RADIUS_METERS));
  const recommendationScore = round((semanticScore * 0.7) + (distanceScore * 0.3), 4);
  const bestIntent = intents[0];

  return {
    placeId: properties.place_id || null,
    name,
    label: address || name,
    address,
    lat,
    lng: lon,
    distanceMeters: Math.round(distanceMeters),
    categories,
    categoryLabel: bestIntent?.label || "Dịch vụ gần đây",
    similarityScore: round(semanticScore, 4),
    recommendationScore,
    reason: buildRecommendationReason(bestIntent, distanceMeters)
  };
}

function calculateCategoryMatchScore(categories, intents) {
  if (!intents.length || !categories.length) {
    return 0.35;
  }

  let bestScore = 0;
  for (const intent of intents) {
    for (const category of categories) {
      for (const expectedCategory of intent.categories) {
        if (category === expectedCategory) {
          bestScore = Math.max(bestScore, intent.score);
        } else if (category.startsWith(expectedCategory) || expectedCategory.startsWith(category)) {
          bestScore = Math.max(bestScore, intent.score * 0.85);
        }
      }
    }
  }

  return bestScore || 0.4;
}

function calculateTextSimilarity(query, text) {
  const queryTokens = tokenize(normalizeText(query));
  const textTokens = tokenize(normalizeText(text));
  if (!queryTokens.length || !textTokens.length) {
    return 0;
  }

  const sharedTokens = queryTokens.filter((token) => textTokens.includes(token));
  return sharedTokens.length / queryTokens.length;
}

function calculateIntentPlaceTextScore(text, intents) {
  const normalizedText = normalizeText(text);
  const textTokens = tokenize(normalizedText);
  let bestScore = 0;

  for (const intent of intents) {
    for (const keyword of intent.placeKeywords || []) {
      const normalizedKeyword = normalizeText(keyword);
      if (!normalizedKeyword) {
        continue;
      }

      if (keywordMatchesText(normalizedKeyword, normalizedText, textTokens)) {
        bestScore = Math.max(bestScore, intent.score);
        continue;
      }

      const keywordTokens = tokenize(normalizedKeyword);
      const sharedTokenCount = keywordTokens.filter((token) => textTokens.includes(token)).length;
      if (sharedTokenCount > 0) {
        bestScore = Math.max(bestScore, intent.score * (sharedTokenCount / keywordTokens.length));
      }
    }
  }

  return bestScore;
}

function passesIntentSpecificFilter(categories, placeText, intents) {
  const intent = intents[0];
  if (!intent?.requirePlaceKeywordOrStrongCategory) {
    return true;
  }

  const normalizedPlaceText = normalizeText(placeText);
  const placeTokens = tokenize(normalizedPlaceText);
  const hasPlaceKeyword = (intent.placeKeywords || [])
    .some((keyword) => keywordMatchesText(normalizeText(keyword), normalizedPlaceText, placeTokens));
  const hasStrongCategory = categories.some((category) =>
    (intent.strongCategories || []).some((strongCategory) =>
      category === strongCategory || category.startsWith(`${strongCategory}.`)
    )
  );

  return hasPlaceKeyword || hasStrongCategory;
}

function keywordMatchesText(normalizedKeyword, normalizedText, textTokens) {
  if (!normalizedKeyword) {
    return false;
  }

  const keywordTokens = tokenize(normalizedKeyword);
  if (keywordTokens.length > 1) {
    return normalizedText.includes(normalizedKeyword);
  }

  if (normalizedKeyword.length <= 3) {
    return textTokens.includes(normalizedKeyword);
  }

  return normalizedText.includes(normalizedKeyword);
}

function buildRecommendationReason(intent, distanceMeters) {
  const distanceLabel = distanceMeters >= 1000
    ? `${round(distanceMeters / 1000, 1)} km`
    : `${Math.max(1, Math.round(distanceMeters))} m`;

  if (intent) {
    return `Phù hợp nhóm ${intent.label.toLowerCase()}, cách khoảng ${distanceLabel}`;
  }

  return `Có tên hoặc loại hình gần với nội dung tìm kiếm, cách khoảng ${distanceLabel}`;
}

function normalizeRadius(value) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return DEFAULT_RECOMMENDATION_RADIUS_METERS;
  }

  return Math.min(Math.round(parsed), DEFAULT_RECOMMENDATION_RADIUS_METERS);
}

function normalizeLimit(value) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return DEFAULT_RECOMMENDATION_LIMIT;
  }

  return Math.min(parsed, MAX_RECOMMENDATION_LIMIT);
}

function isValidCoordinate(lat, lng) {
  return Number.isFinite(lat)
    && Number.isFinite(lng)
    && lat >= -90
    && lat <= 90
    && lng >= -180
    && lng <= 180
    && !(Math.abs(lat) < 0.000001 && Math.abs(lng) < 0.000001);
}

function calculateDistanceMeters(lat1, lng1, lat2, lng2) {
  const earthRadiusMeters = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadiusMeters * c;
}

function normalizeText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function tokenize(value) {
  return normalizeText(value)
    .split(" ")
    .filter((token) => token.length > 1);
}

function round(value, decimals) {
  const factor = 10 ** decimals;
  return Math.round(Number(value || 0) * factor) / factor;
}

module.exports = {
  autocomplete,
  geocode,
  recommendNearby,
  findPlaceByQuery
};

function getCachedAutocomplete(query) {
  const cacheKey = query.toLowerCase();
  const cached = autocompleteCache.get(cacheKey);
  if (!cached) {
    return null;
  }

  if (cached.expiresAt <= Date.now()) {
    autocompleteCache.delete(cacheKey);
    return null;
  }

  return cached.value;
}

function setCachedAutocomplete(query, value) {
  const cacheKey = query.toLowerCase();
  autocompleteCache.set(cacheKey, {
    value,
    expiresAt: Date.now() + Math.max(1000, env.autocompleteCacheTtlMs)
  });
}
