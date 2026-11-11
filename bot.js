const axios = require('axios');

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

async function askGemini(questionText, options = []) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=${GEMINI_API_KEY}`;
    
    const prompt = `
    Jawab soal kuis berikut secara tepat dan singkat!
    Pertanyaan: ${questionText}
    Pilihan: ${options.join(', ')}
    
    Berikan HANYA teks pilihan jawaban yang benar tanpa penjelasan tambahan!
    `;

    try {
        const response = await axios.post(url, {
            contents: [{ parts: [{ text: prompt }] }]
        });

        const answer = response.data.candidates[0].content.parts[0].text.trim();
        console.log("\n🤖 Jawaban dari Gemini:", answer);
        return answer;
    } catch (error) {
        console.error("❌ Error saat menghubungkan ke Gemini:", error.response?.data || error.message);
    }
}

// Uji coba pertanyaan kuis contoh
const contohSoal = "Apa ibu kota negara Indonesia?";
const contohPilihan = ["A. Surabaya", "B. Jakarta", "C. Bandung", "D. Medan"];

console.log("📝 Mengirim soal ke Gemini...");
askGemini(contohSoal, contohPilihan);


