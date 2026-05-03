# Firestore Catalog Setup Guide

## Overview
PriceCart does not ship any catalog seed data in the app or repo. Product search and product detail screens read only from Firestore.

The catalog must exist in these collections:

- `stores`
- `store_prices`

If either collection is empty or incomplete, the app will surface a catalog setup problem instead of showing misleading search results.

## Required Firestore Structure

### `stores/{storeId}`
Each store document must contain:

- `storeName`
- `latitude`
- `longitude`
- `neighborhood`
- `address`
- `hours`
- `phoneNumber`
- `note`

### `store_prices/{priceId}`
Each price document must contain:

- `storeId`
- `productName`
- `productNameLowercase`
- `price`
- `currency`
- `updatedAt` (optional Firestore `Timestamp`)

## Relationship Rules

- Every `store_prices.storeId` must reference an existing `stores/{storeId}` document.
- `productNameLowercase` should be the lowercase searchable form of `productName`.
- Missing linked store records or malformed catalog documents will cause search to report a backend catalog issue.

## Verification Checklist

1. Publish [firestore.rules](/C:/Users/barre/AndroidStudioProjects/PriceCart/firestore.rules).
2. Confirm `stores` exists and contains complete store documents.
3. Confirm `store_prices` exists and contains complete price documents.
4. Confirm each `store_prices.storeId` matches a real `stores/{storeId}` document id.
5. Run a search such as `milk` in the app and verify store results appear.

## Notes

- User-specific collections under `users/{uid}` are still created by the app:
  - `favourites`
  - `recent_searches`
  - `recently_viewed`
- No frontend fallback catalog, embedded sample JSON, or app-side Firestore initializer is supported.
