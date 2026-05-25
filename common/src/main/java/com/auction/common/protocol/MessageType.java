package com.auction.common.protocol;

public enum MessageType {
    // ── Bidding ───────────────────────────────────────────────
    PLACE_BID_REQ,
    PLACE_BID_RES,
    BID_UPDATED_EVENT,

    // ── AutoBid ───────────────────────────────────────────────
    REGISTER_AUTOBID_REQ,
    REGISTER_AUTOBID_RES,
    CANCEL_AUTOBID_REQ,
    CANCEL_AUTOBID_RES,
    GET_AUTOBID_REQ,
    GET_AUTOBID_RES,

    // ── Auth ──────────────────────────────────────────────────
    LOGIN_REQ,
    LOGIN_RES,
    REGISTER_REQ,
    REGISTER_RES,

    // ── Auctions ──────────────────────────────────────────────
    LIST_AUCTIONS_REQ,
    LIST_AUCTIONS_RES,
    AUCTION_CLOSED_EVENT,
    SUBSCRIBE_REQ,
    UNSUBSCRIBE_REQ,

    // ── Seller ────────────────────────────────────────────────
    SUBMIT_LISTING_REQ,
    SUBMIT_LISTING_RES,

    // ── Admin ─────────────────────────────────────────────────
    ADMIN_ACTION_REQ,
    ADMIN_ACTION_RES,
    LIST_USERS_REQ,
    LIST_USERS_RES,
    UPDATE_USER_REQ,
    UPDATE_USER_RES,

    // ── Wallet ────────────────────────────────────────────────
    DEPOSIT_REQ,
    DEPOSIT_RES,
    WITHDRAW_REQ,
    WITHDRAW_RES,

    // ── Errors ────────────────────────────────────────────────
    ERROR_RES
}
