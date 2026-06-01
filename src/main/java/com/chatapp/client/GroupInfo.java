package com.chatapp.client;

import java.util.List;

// Snapshot of a group's owner + member list, returned by the server in
// response to a GROUP_QUERY (delivered as GROUP_INFO).
public record GroupInfo(String groupId, String owner, List<String> members) {}
