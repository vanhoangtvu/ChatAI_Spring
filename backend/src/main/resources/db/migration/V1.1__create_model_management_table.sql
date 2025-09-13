-- Create model_management table
CREATE TABLE IF NOT EXISTS model_management (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id VARCHAR(255) NOT NULL UNIQUE,
    model_name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reason TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    priority INT DEFAULT 0,
    groq_model_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(255)
);

-- Create index for better performance
CREATE INDEX idx_model_management_enabled ON model_management(is_enabled);
CREATE INDEX idx_model_management_category ON model_management(category);
CREATE INDEX idx_model_management_priority ON model_management(priority);

-- Insert default models
INSERT INTO model_management (model_id, model_name, description, category, is_enabled, is_default, priority, updated_by) VALUES
('llama-3.1-8b-instant', 'Llama 3.1 8B Instant', 'Fastest model, good for quick responses', 'Llama', TRUE, TRUE, 100, 'system'),
('llama-3.3-70b-versatile', 'Llama 3.3 70B Versatile', 'Latest Llama model, most capable', 'Llama', TRUE, TRUE, 90, 'system'),
('gemma2-9b-it', 'Gemma2 9B', 'Google''s Gemma2 model, 9B parameters', 'Google', TRUE, FALSE, 80, 'system'),
('deepseek-r1-distill-llama-70b', 'DeepSeek R1 Distill', 'DeepSeek''s distilled model based on Llama 70B', 'DeepSeek', TRUE, FALSE, 70, 'system'),
('llama-4-maverick-17b', 'Llama 4 Maverick 17B', 'Meta''s latest Llama 4 model', 'Llama', TRUE, FALSE, 60, 'system'),
('llama-4-scout-17b', 'Llama 4 Scout 17B', 'Meta''s Scout model for instruction following', 'Llama', TRUE, FALSE, 50, 'system'),
('qwen3-32b', 'Qwen 3 32B', 'Alibaba''s Qwen model, 32B parameters', 'Qwen', TRUE, FALSE, 40, 'system'),
('kimi-k2-instruct', 'Kimi K2 Instruct', 'Moonshot AI''s Kimi model', 'Kimi', TRUE, FALSE, 30, 'system'),
('compound-beta', 'Compound Beta', 'Compound AI''s beta model', 'Compound', TRUE, FALSE, 20, 'system'),
('compound-beta-mini', 'Compound Beta Mini', 'Compound AI''s smaller beta model', 'Compound', TRUE, FALSE, 10, 'system'),
('gpt-oss-20b', 'GPT-OSS 20B', 'OpenAI''s open source model, 20B parameters', 'OpenAI', TRUE, FALSE, 5, 'system'),
('gpt-oss-120b', 'GPT-OSS 120B', 'OpenAI''s open source model, 120B parameters', 'OpenAI', TRUE, FALSE, 1, 'system');
