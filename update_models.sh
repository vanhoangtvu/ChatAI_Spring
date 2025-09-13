#!/bin/bash

# Script to get official Groq models and update project
# Usage: ./update_models.sh

GROQ_API_KEY="${GROQ_API_KEY:-your-groq-api-key-here}"

echo "🔍 Fetching official models from Groq API..."

# Get models from Groq API
response=$(curl -s -H "Authorization: Bearer $GROQ_API_KEY" \
    -H "Content-Type: application/json" \
    https://api.groq.com/openai/v1/models)

if [ $? -eq 0 ]; then
    echo "✅ Successfully fetched models from Groq"
    
    # Extract model IDs and create a clean list
    echo "$response" | jq -r '.data[]?.id // empty' 2>/dev/null | sort > /tmp/groq_models.txt
    
    if [ -s /tmp/groq_models.txt ]; then
        echo ""
        echo "📋 Available models on Groq:"
        cat /tmp/groq_models.txt
        
        echo ""
        echo "📊 Total models found: $(wc -l < /tmp/groq_models.txt)"
        
        # Create SQL to update database
        echo ""
        echo "🔧 Generating SQL to update database..."
        
        cat > /tmp/update_models.sql << 'EOF'
-- Disable all models first
UPDATE model_management SET is_enabled = 0, reason = 'Model not available on Groq API', updated_at = NOW();

-- Enable only verified working models
EOF
        
        # Add verified models
        while read -r model_id; do
            case $model_id in
                "llama-3.1-8b-instant"|"llama3-8b-8192"|"llama-3.2-1b-preview"|"llama-3.2-3b-preview"|"llama-3.2-11b-text-preview"|"llama-3.2-90b-text-preview"|"llama3-70b-8192"|"mixtral-8x7b-32768"|"gemma-7b-it"|"gemma2-9b-it")
                    echo "UPDATE model_management SET is_enabled = 1, reason = NULL, updated_at = NOW() WHERE model_id = '$model_id';" >> /tmp/update_models.sql
                    ;;
            esac
        done < /tmp/groq_models.txt
        
        # Set default model
        echo "UPDATE model_management SET is_default = 0;" >> /tmp/update_models.sql
        echo "UPDATE model_management SET is_default = 1 WHERE model_id = 'llama-3.1-8b-instant' AND is_enabled = 1;" >> /tmp/update_models.sql
        echo "SELECT 'Models updated successfully' as status;" >> /tmp/update_models.sql
        
        echo "SQL file created: /tmp/update_models.sql"
        echo ""
        echo "🚀 Would you like to apply these changes to the database? (y/n)"
        read -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            mysql -u root -p1111 -D db_AIchatbot < /tmp/update_models.sql
            echo "✅ Database updated!"
        fi
    else
        echo "❌ No models found in API response"
    fi
else
    echo "❌ Failed to fetch models from Groq API"
fi

# Clean up
rm -f /tmp/groq_models.txt /tmp/update_models.sql 2>/dev/null
