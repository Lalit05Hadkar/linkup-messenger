-- Harden RLS Policies Migration
-- Revoke anon access, implement strict per-table auth.uid() checks, and add supporting indexes.

-- 1. Revoke anon role access from all public tables and sequences
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;

-- Ensure authenticated role has necessary table privileges
GRANT USAGE ON SCHEMA public TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON users TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON friend_requests TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON chats TO authenticated;
GRANT SELECT, INSERT, DELETE ON messages TO authenticated;
GRANT UPDATE (status) ON messages TO authenticated;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authenticated;

-- 2. Drop legacy permissive allow_all_* policies
DROP POLICY IF EXISTS "allow_all_users" ON users;
DROP POLICY IF EXISTS "allow_all_friend_requests" ON friend_requests;
DROP POLICY IF EXISTS "allow_all_chats" ON chats;
DROP POLICY IF EXISTS "allow_all_messages" ON messages;

-- 3. Users Table Policies
-- Authenticated users can search and read all user profiles (needed for discovery & friends list)
CREATE POLICY "users_read_authenticated" ON users
    FOR SELECT TO authenticated
    USING (true);

-- Users can only insert their own row matching auth.uid()
CREATE POLICY "users_insert_own_row" ON users
    FOR INSERT TO authenticated
    WITH CHECK (id = (auth.uid())::text);

-- Users can only update their own row matching auth.uid()
CREATE POLICY "users_update_own_row" ON users
    FOR UPDATE TO authenticated
    USING (id = (auth.uid())::text)
    WITH CHECK (id = (auth.uid())::text);

-- Users can only delete their own profile row
CREATE POLICY "users_delete_own_row" ON users
    FOR DELETE TO authenticated
    USING (id = (auth.uid())::text);

-- 4. Friend Requests Table Policies
-- Only the sender or the receiver can read a friend request
CREATE POLICY "friend_requests_select_participants" ON friend_requests
    FOR SELECT TO authenticated
    USING (
        sender_id = (auth.uid())::text
        OR receiver_id = (auth.uid())::text
    );

-- Users can only insert friend requests where they are the sender
CREATE POLICY "friend_requests_insert_sender" ON friend_requests
    FOR INSERT TO authenticated
    WITH CHECK (sender_id = (auth.uid())::text);

-- Only the receiver can update a friend request status (accept / reject)
CREATE POLICY "friend_requests_update_receiver_only" ON friend_requests
    FOR UPDATE TO authenticated
    USING (receiver_id = (auth.uid())::text)
    WITH CHECK (receiver_id = (auth.uid())::text);

-- Either participant can cancel/delete a friend request
CREATE POLICY "friend_requests_delete_participants" ON friend_requests
    FOR DELETE TO authenticated
    USING (
        sender_id = (auth.uid())::text
        OR receiver_id = (auth.uid())::text
    );

-- 5. Chats Table Policies
-- Only participants can view their chats
CREATE POLICY "chats_select_participants_only" ON chats
    FOR SELECT TO authenticated
    USING ((auth.uid())::text = ANY(participants));

-- Only participants can create a chat containing themselves
CREATE POLICY "chats_insert_participants_only" ON chats
    FOR INSERT TO authenticated
    WITH CHECK ((auth.uid())::text = ANY(participants));

-- Only participants can update chat metadata (last message, time, unread counts)
CREATE POLICY "chats_update_participants_only" ON chats
    FOR UPDATE TO authenticated
    USING ((auth.uid())::text = ANY(participants))
    WITH CHECK ((auth.uid())::text = ANY(participants));

-- Only participants can delete a chat
CREATE POLICY "chats_delete_participants_only" ON chats
    FOR DELETE TO authenticated
    USING ((auth.uid())::text = ANY(participants));

-- 6. Messages Table Policies
-- Only participants of the parent chat can read messages
CREATE POLICY "messages_select_chat_participants" ON messages
    FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM chats
            WHERE chats.id = messages.chat_id
              AND (auth.uid())::text = ANY(chats.participants)
        )
    );

-- Users can only insert messages as their own sender_id AND only if they are participants of the chat
CREATE POLICY "messages_insert_own_sender_participant" ON messages
    FOR INSERT TO authenticated
    WITH CHECK (
        sender_id = (auth.uid())::text
        AND EXISTS (
            SELECT 1 FROM chats
            WHERE chats.id = messages.chat_id
              AND (auth.uid())::text = ANY(chats.participants)
        )
    );

-- Only participants can update message status (e.g. delivered, seen)
CREATE POLICY "messages_update_status_participants" ON messages
    FOR UPDATE TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM chats
            WHERE chats.id = messages.chat_id
              AND (auth.uid())::text = ANY(chats.participants)
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM chats
            WHERE chats.id = messages.chat_id
              AND (auth.uid())::text = ANY(chats.participants)
        )
    );

-- Users can delete their own messages if they belong to the chat
CREATE POLICY "messages_delete_own_messages" ON messages
    FOR DELETE TO authenticated
    USING (
        sender_id = (auth.uid())::text
        AND EXISTS (
            SELECT 1 FROM chats
            WHERE chats.id = messages.chat_id
              AND (auth.uid())::text = ANY(chats.participants)
        )
    );

-- 7. Indexes to optimize participant checks and queries
CREATE INDEX IF NOT EXISTS idx_messages_chat_sender ON messages(chat_id, sender_id);
CREATE INDEX IF NOT EXISTS idx_friend_requests_sender_receiver ON friend_requests(sender_id, receiver_id);
