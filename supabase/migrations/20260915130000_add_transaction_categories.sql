-- Migration: Add transaction_categories table for custom Income/Expense categories
-- Run this in Supabase SQL Editor or via supabase CLI

CREATE TABLE IF NOT EXISTS public.transaction_categories (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    is_custom BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, name, type)
);

ALTER TABLE public.transaction_categories ENABLE ROW LEVEL SECURITY;

-- Users can view their own categories
CREATE POLICY "Users can view own categories"
    ON public.transaction_categories
    FOR SELECT
    USING (auth.uid() = user_id);

-- Users can insert their own categories
CREATE POLICY "Users can insert own categories"
    ON public.transaction_categories
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Users can update their own categories
CREATE POLICY "Users can update own categories"
    ON public.transaction_categories
    FOR UPDATE
    USING (auth.uid() = user_id);

-- Users can delete their own categories
CREATE POLICY "Users can delete own categories"
    ON public.transaction_categories
    FOR DELETE
    USING (auth.uid() = user_id);

-- Index for faster queries by user and type
CREATE INDEX IF NOT EXISTS idx_transaction_categories_user_type
    ON public.transaction_categories (user_id, type);

-- Insert default categories for a new user (to be called by a trigger or app logic)
-- Note: This is just an example; you may want to insert via app or Edge Function.
-- We'll leave it to the app to insert default categories on first run.

-- Update the updated_at on modification
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_transaction_categories_updated_at
    BEFORE UPDATE ON public.transaction_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();