import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createOrder } from "../services/orders";
import { fetchRestaurantById } from "../services/restaurants";
import { useAuth } from "../context/AuthContext";
import { getUserAddresses } from "../services/addresses";
import "./Checkout.css";

const ORDER_PAYMENT_META_PREFIX = "orderPaymentMeta_";

export default function Checkout({ cart, setCart }) {
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const total = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  
  // Lưu ý: restaurantId trong cart thực chất là merchantId
  const merchantId = cart.length > 0 ? (cart[0].restaurantId || cart[0].merchantId) : null;
  const restaurantName = cart.length > 0 ? (cart[0].restaurantName || "Nhà hàng") : "Nhà hàng";
  const restaurantAddressFromCart =
    cart.length > 0 ? (cart[0].restaurantAddress || cart[0].restaurant_address || "") : "";

  const [restaurantDetails, setRestaurantDetails] = useState(null);
  const [form, setForm] = useState({
    lastName: "",
    firstName: "",
    phone: "",
    address: "",
    ward: "",
    district: "",
    city: "Ho Chi Minh", // Default
  });

  const [paymentMethod, setPaymentMethod] = useState("cod"); // 💳 Thêm trạng thái thanh toán
  const [isProcessing, setIsProcessing] = useState(false);
  const [showSuccessPopup, setShowSuccessPopup] = useState(false);
  const [customerCoords, setCustomerCoords] = useState(null);
  const [manualCoords, setManualCoords] = useState({ lat: "", lng: "" });
  const [useManualCoords, setUseManualCoords] = useState(false);
  const [geocodingStatus, setGeocodingStatus] = useState(""); // "searching", "found", "not_found", ""
  const [geocodingResult, setGeocodingResult] = useState(null); // Lưu thông tin ward/district/city từ geocoding
  const [savedAddresses, setSavedAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);

  // Địa chỉ đã lưu hiện đang được chọn (nếu có)
  const selectedSavedAddress = savedAddresses.find(addr => addr.id === selectedAddressId);

  // ==== Auto-fill thông tin user ====
  useEffect(() => {
    if (currentUser) {
      setForm(prev => ({
        ...prev,
        lastName: currentUser.lastname || prev.lastName,
        firstName: currentUser.firstname || prev.firstName,
        phone: currentUser.phonenumber || prev.phone,
        address: currentUser.address || prev.address,
      }));
    }
  }, [currentUser]);

  useEffect(() => {
    const fetchAddresses = async () => {
      try {
        const data = await getUserAddresses();
        setSavedAddresses(data);
        if (data.length > 0) {
          setSelectedAddressId(data[0].id);
          setForm(prev => ({
            ...prev,
            address: data[0].fullAddress || data[0].street || prev.address
          }));
          if (data[0].lat && data[0].lng) {
            const coords = { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lng) };
            setCustomerCoords(coords);
            setGeocodingStatus("found");
            setGeocodingResult({
              lat: coords.lat,
              lng: coords.lng,
              ward: data[0].communeName,
              district: data[0].districtName,
              city: data[0].provinceName,
              displayName: data[0].fullAddress
            });
          }
        }
      } catch (err) {
        console.warn("Không thể tải danh sách địa chỉ đã lưu:", err);
      }
    };
    fetchAddresses();
  }, []);

  useEffect(() => {
    if (selectedSavedAddress) {
      setForm(prev => ({
        ...prev,
        address: selectedSavedAddress.fullAddress || selectedSavedAddress.street || prev.address
      }));
    }
  }, [selectedSavedAddress]);

  // ==== Lấy thông tin nhà hàng ====
  // Lưu ý: restaurantId trong cart thực chất là merchantId
  // Không cần fetch restaurant từ backend, chỉ cần merchantId + tên/địa chỉ từ cart
  useEffect(() => {
    if (merchantId) {
      // Dùng thông tin từ cart, không cần fetch từ backend
      setRestaurantDetails({
        id: merchantId,
        merchantId: merchantId,
        name: restaurantName,
        address: restaurantAddressFromCart || ""
      });
      }
  }, [merchantId, restaurantName, restaurantAddressFromCart]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  };

  // === Geocoding với Nominatim (cải thiện với nhiều cách thử) ===
  const getCoordinatesForAddress = async (address, skipFallback = false) => {
    if (!address || address.trim().length < 5) {
      return null;
    }

    setGeocodingStatus("searching");
    const trimmedAddress = address.trim();
    
    // Phát hiện loại địa chỉ:
    // 1. City only (như "TP.HCM") - KHÔNG extract ward/district
    // 2. Detailed address (có số nhà/ward/district) - Extract ward/district
    // 3. Landmark/Place (như "đại học sài gòn") - VẪN extract ward/district từ geocoding
    const isCityOnly = (addr) => {
      const cityOnlyPatterns = /^(tp\.?hcm|tp\.?hồ\s*chí\s*minh|ho\s*chi\s*minh|hồ\s*chí\s*minh)$/i;
      return cityOnlyPatterns.test(addr.trim());
    };
    
    const isDetailedAddress = (addr) => {
      // Có số nhà (số ở đầu)
      const hasHouseNumber = /^\d+/.test(addr.trim());
      // Có từ khóa ward/district
      const hasWardDistrict = /(phường|xã|quận|huyện)/i.test(addr);
      
      return hasHouseNumber || hasWardDistrict;
    };
    
    const isCityOnlyInput = isCityOnly(trimmedAddress);
    const isDetailed = isDetailedAddress(trimmedAddress);
    const shouldExtractWardDistrict = isDetailed || !isCityOnlyInput; // Extract nếu detailed HOẶC không phải chỉ là city
    
    console.log(`🔍 Địa chỉ "${trimmedAddress}":`, {
      isCityOnly: isCityOnlyInput,
      isDetailed: isDetailed,
      shouldExtractWardDistrict: shouldExtractWardDistrict
    });
    
    // Chiến lược: Thử tìm đường trước (không có số nhà), sau đó mới thử với số nhà
    // Vì địa chỉ chi tiết quá có thể không tìm thấy trong Nominatim
    const extractStreetName = (addr) => {
      // Chỉ lấy phần trước dấu phẩy đầu tiên để tránh ăn luôn "phường ..."
      // Ví dụ: "số 4 đường 30, phường hiệp bình" -> "số 4 đường 30"
      const firstPart = addr.split(',')[0].trim();
      // Tách số nhà và tên đường: "125/30/2 Tây Lân" -> "Tây Lân"
      const match = firstPart.match(/\d+[\/\d]*\s*(.+)/);
      return match ? match[1].trim() : firstPart;
    };
    
    const streetName = extractStreetName(trimmedAddress);
    
    // Tạo nhiều biến thể địa chỉ để thử
    // Với địa danh/địa điểm, ưu tiên query trực tiếp với tên địa điểm
    const addressVariations = [];
    
    // Nếu là địa danh/địa điểm (không phải city only và không có số nhà)
    if (!isCityOnlyInput && !isDetailed) {
      // Ưu tiên query trực tiếp với tên địa điểm + HCM
      addressVariations.push(
        `${trimmedAddress}, Ho Chi Minh City, Vietnam`,
        `${trimmedAddress}, Thành phố Hồ Chí Minh, Vietnam`,
        `${trimmedAddress}, TP. Hồ Chí Minh, Vietnam`,
        `${trimmedAddress}, TP.HCM, Vietnam`
      );
    }
    
    // Thêm các biến thể khác (KHÔNG cố định vào Bình Tân / Bình Trị Đông A để tránh lệch khu vực)
    addressVariations.push(
      // 1. Thử tìm đường (không số nhà) + HCM
      `${streetName}, Ho Chi Minh City, Vietnam`,
      `${streetName}, Thành phố Hồ Chí Minh, Vietnam`,
      `${streetName}, TP. Hồ Chí Minh, Vietnam`,
      `${streetName}, TP.HCM, Vietnam`,
      
      // 2. Thử với địa chỉ đầy đủ (có số nhà) + HCM
      `${trimmedAddress}, Ho Chi Minh City, Vietnam`,
      `${trimmedAddress}, Thành phố Hồ Chí Minh, Vietnam`,
      `${trimmedAddress}, TP. Hồ Chí Minh, Vietnam`,
      `${trimmedAddress}, TP.HCM, Vietnam`,
      
      // 3. Thử chỉ với Việt Nam (fallback rất rộng)
      `${trimmedAddress}, Vietnam`
    );
    
    // Loại bỏ duplicate và null
    const uniqueVariations = [...new Set(addressVariations.filter(Boolean))];

    // Tách sẵn các keyword từ input để dùng cho scoring và kiểm tra liên quan
    const inputLower = trimmedAddress.toLowerCase();
    const inputWords = inputLower.split(/\s+/).filter(w => w.length > 2);

    console.log(`🔍 Bắt đầu geocoding cho địa chỉ: "${trimmedAddress}"`);
    console.log(`📋 Tên đường: "${streetName}"`);
    console.log(`📋 Sẽ thử ${uniqueVariations.length} biến thể địa chỉ`);

    for (let i = 0; i < uniqueVariations.length; i++) {
      const query = uniqueVariations[i];
      try {
        // Nominatim yêu cầu delay ít nhất 1 giây giữa các request
        if (i > 0) {
          await new Promise(resolve => setTimeout(resolve, 1100));
        }
        
        const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=3&countrycodes=vn&addressdetails=1&extratags=1`;
        
        console.log(`🔄 [${i + 1}/${uniqueVariations.length}] Thử query: ${query}`);
        
        const res = await fetch(url, {
          headers: {
            'Accept': 'application/json',
            'User-Agent': 'FastfoodDeliveryApp/1.0 (contact: support@fastfooddelivery.com)',
            'Referer': window.location.origin
          }
        });
        
        if (!res.ok) {
          console.warn(`⚠️ [${i + 1}] Geocoding failed, status: ${res.status}, statusText: ${res.statusText}`);
          if (res.status === 429) {
            console.warn(`⏳ Rate limit hit, đợi thêm 2 giây...`);
            await new Promise(resolve => setTimeout(resolve, 2000));
          }
          continue;
        }

      const data = await res.json();
        console.log(`📥 [${i + 1}] Nhận được ${data?.length || 0} kết quả`);
        
      if (Array.isArray(data) && data.length > 0) {
          // Tìm kết quả phù hợp nhất (có thể có nhiều kết quả)
          // Với địa danh/địa điểm, ưu tiên kết quả có tên gần với input nhất
          let bestResult = null;
          let bestScore = -1;
          
          for (const result of data) {
            const lat = parseFloat(result.lat);
            const lng = parseFloat(result.lon);
            
            if (!isFinite(lat) || !isFinite(lng)) continue;
            
            // Kiểm tra xem kết quả có trong HCM không
            const isInHCM = result.address?.city?.toLowerCase().includes('hồ chí minh') ||
                           result.address?.state?.toLowerCase().includes('hồ chí minh') ||
                           result.display_name?.toLowerCase().includes('hồ chí minh') ||
                           result.display_name?.toLowerCase().includes('ho chi minh');
            
            if (!isInHCM && i !== uniqueVariations.length - 1) continue;
            
            // Tính điểm phù hợp: ưu tiên kết quả có tên gần với input
            let score = result.importance || 0;
            const displayNameLower = (result.display_name || '').toLowerCase();
            const amenityLower = (result.address?.amenity || '').toLowerCase();
            const nameLower = (result.name || '').toLowerCase();
            
            // Nếu tên địa điểm chứa từ khóa trong input, tăng điểm
            let matchCount = 0;
            for (const word of inputWords) {
              if (displayNameLower.includes(word) || amenityLower.includes(word) || nameLower.includes(word)) {
                matchCount++;
              }
            }
            
            // Tăng điểm dựa trên số từ khóa khớp
            if (matchCount > 0) {
              score += (matchCount / inputWords.length) * 2.0; // Tăng điểm đáng kể nếu nhiều từ khóa khớp
            }
            
            // Nếu là địa danh/địa điểm (không phải city only), ưu tiên kết quả có amenity
            if (!isCityOnlyInput && result.address?.amenity) {
              score += 0.5;
            }
            
            // Ưu tiên kết quả có tên chính xác hơn (không phải chỉ là một phần của địa chỉ)
            if (amenityLower === inputLower || nameLower === inputLower) {
              score += 3.0; // Tăng điểm rất nhiều nếu tên chính xác
            }
            
            if (score > bestScore) {
              bestScore = score;
              bestResult = result;
            }
          }
          
          if (bestResult) {
            // Trước khi chấp nhận, kiểm tra lại mức độ liên quan với input.
            const addressInfo = bestResult.address || {};
            const displayNameLower = (bestResult.display_name || '').toLowerCase();
            const suburbLower = (addressInfo.suburb || addressInfo.quarter || '').toLowerCase();

            const hasKeywordMatch = inputWords.some(w => 
              displayNameLower.includes(w) || suburbLower.includes(w)
            );

            // Ngưỡng tối thiểu: phải có ít nhất một keyword trùng
            // và điểm phù hợp không quá thấp, nếu không coi như không hợp lệ.
            if (!hasKeywordMatch || bestScore < 1.0) {
              console.warn(
                `⚠️ [${i + 1}] Kết quả geocoding không đủ liên quan tới input (hasKeywordMatch=${hasKeywordMatch}, score=${bestScore.toFixed(2)}). Bỏ qua và thử biến thể tiếp theo.`
              );
              continue;
            }
            const lat = parseFloat(bestResult.lat);
            const lng = parseFloat(bestResult.lon);
            
            console.log(`✅ Geocoding thành công ở lần thử ${i + 1}!`);
            console.log(`📍 Tọa độ: (${lat}, ${lng})`);
            console.log(`📍 Địa chỉ tìm thấy: ${bestResult.display_name}`);
            console.log(`📍 Độ chính xác: ${bestResult.importance || 'N/A'}`);
            console.log(`📍 Điểm phù hợp: ${bestScore.toFixed(2)}`);
            if (bestResult.address?.amenity) {
              console.log(`📍 Địa điểm: ${bestResult.address.amenity}`);
            }
            
            // Extract ward, district, city từ kết quả geocoding
            
            // City/Thành phố: luôn extract
            // LƯU Ý: Với Việt Nam, có thể có nhiều cấp độ city:
            // - addressInfo.city có thể là "Thủ Đức" (thành phố trực thuộc trung ương)
            // - addressInfo.state có thể là "Ho Chi Minh City" (thành phố cấp trên)
            // - display_name có thể chứa "Ho Chi Minh City" ở cuối
            let extractedCity = addressInfo.city || 
                               addressInfo.town || 
                               addressInfo.state || 
                               '';
            
            // Nếu city là "Thủ Đức" hoặc các thành phố trực thuộc trung ương khác,
            // ưu tiên dùng state (thường là "Ho Chi Minh City")
            const cityLower = extractedCity.toLowerCase();
            const specialCities = ['thủ đức', 'thu duc'];
            
            if (specialCities.some(sc => cityLower.includes(sc))) {
              console.log(`⚠️ [DEBUG] City là "${extractedCity}" (thành phố trực thuộc), thử dùng state hoặc parse từ display_name`);
              
              // Ưu tiên state
              if (addressInfo.state) {
                extractedCity = addressInfo.state;
                console.log(`✅ [DEBUG] Dùng state làm city: "${extractedCity}"`);
              } else {
                // Parse từ display_name: tìm "Ho Chi Minh City" hoặc "Thành phố Hồ Chí Minh"
                const displayName = bestResult.display_name || '';
                const hcmPatterns = [
                  /Ho\s+Chi\s+Minh\s+City/i,
                  /Thành\s+phố\s+Hồ\s+Chí\s+Minh/i,
                  /TP\.?\s*Hồ\s+Chí\s+Minh/i,
                  /TP\.?HCM/i,
                ];
                
                for (const pattern of hcmPatterns) {
                  const match = displayName.match(pattern);
                  if (match) {
                    extractedCity = match[0].trim();
                    console.log(`✅ [DEBUG] Tìm thấy city từ display_name: "${extractedCity}"`);
                    break;
                  }
                }
                
                // Nếu vẫn không tìm thấy, dùng default
                if (!extractedCity || specialCities.some(sc => extractedCity.toLowerCase().includes(sc))) {
                  extractedCity = 'Ho Chi Minh';
                  console.log(`⚠️ [DEBUG] Dùng default city: "${extractedCity}"`);
                }
              }
            }
            
            // Nếu vẫn không có city, dùng default
            if (!extractedCity) {
              extractedCity = 'Ho Chi Minh';
            }
            
            // Ward/Phường và District/Quận: 
            // - Extract nếu địa chỉ có chi tiết (số nhà/ward/district)
            // - HOẶC extract nếu là địa danh/địa điểm (không phải chỉ là city name)
            // - CHỈ KHÔNG extract nếu input CHỈ là city name (như "TP.HCM")
            let finalWard = '';
            let finalDistrict = '';
            
            if (shouldExtractWardDistrict) {
              // Log toàn bộ address object để debug
              console.log(`🔍 [DEBUG] Address object từ Nominatim:`, addressInfo);
              console.log(`🔍 [DEBUG] Tất cả keys trong address object:`, Object.keys(addressInfo));
              
              // Ward/Phường: thử nhiều field có thể (theo thứ tự ưu tiên)
              // Nominatim cho Việt Nam thường dùng: suburb, neighbourhood, quarter, village
              const extractedWard = addressInfo.suburb || 
                                    addressInfo.neighbourhood || 
                                    addressInfo.quarter || 
                                    addressInfo.village || 
                                    addressInfo.municipality || '';
              
              // District/Quận: thử nhiều field có thể
              // Nominatim cho Việt Nam thường dùng: city_district, county, district
              // LƯU Ý: Một số địa chỉ có thể không có district rõ ràng trong Nominatim
              let extractedDistrict = addressInfo.city_district || 
                                     addressInfo.county || 
                                     addressInfo.district || 
                                     addressInfo.state_district || '';
              
              // Nếu district là "Thủ Đức", có thể đây là thành phố trực thuộc trung ương, không phải quận
              // Cần kiểm tra kỹ hơn
              if (extractedDistrict && extractedDistrict.toLowerCase().includes('thủ đức')) {
                console.log(`⚠️ [DEBUG] District là "Thủ Đức" - có thể là thành phố, không phải quận`);
                // Có thể cần bỏ qua hoặc xử lý đặc biệt
              }
              
              finalWard = extractedWard;
              finalDistrict = extractedDistrict;
              
              console.log(`🔍 [DEBUG] Extract từ address object:`, {
                ward: finalWard || '(không có)',
                district: finalDistrict || '(không có)',
                triedFields: {
                  ward: ['suburb', 'neighbourhood', 'quarter', 'village', 'municipality'],
                  district: ['city_district', 'county', 'district', 'state_district']
                },
                allAddressFields: addressInfo
              });
              
              // Nếu không tìm thấy ward/district từ address object, thử parse từ display_name
              if (!finalWard || !finalDistrict) {
                const displayName = bestResult.display_name || '';
                console.log(`🔍 [DEBUG] Parse từ display_name: "${displayName}"`);
                
                // Parse từ display_name với nhiều pattern khác nhau
                // Ví dụ: "Tây Lân, Phường Bình Tân, Ho Chi Minh City, 72031, Vietnam"
                // Hoặc: "125/30/2 Đ. Tây Lân, Bình Trị Đông A, Bình Tân, Thành phố Hồ Chí Minh"
                
                // Ward patterns (ưu tiên)
                const wardPatterns = [
                  /Phường\s+([^,]+)/i,
                  /Xã\s+([^,]+)/i,
                ];
                
                // District patterns (ưu tiên)
                const districtPatterns = [
                  /Quận\s+([^,]+)/i,
                  /Huyện\s+([^,]+)/i,
                ];
                
                // Extract ward
                for (const pattern of wardPatterns) {
                  const match = displayName.match(pattern);
                  if (match && !finalWard) {
                    finalWard = match[1].trim();
                    console.log(`✅ [DEBUG] Tìm thấy ward từ display_name: "${finalWard}"`);
                    break;
                  }
                }
                
                // Extract district
                for (const pattern of districtPatterns) {
                  const match = displayName.match(pattern);
                  if (match && !finalDistrict) {
                    finalDistrict = match[1].trim();
                    console.log(`✅ [DEBUG] Tìm thấy district từ display_name: "${finalDistrict}"`);
                    break;
      }
                }
                
                // Fallback: Nếu có ward nhưng không có district, thử tìm từ context
                // Ví dụ: "Phường Bình Tân" - có thể "Bình Tân" là tên quận
              if (finalWard && !finalDistrict) {
                  console.log(`🔍 [DEBUG] Có ward "${finalWard}" nhưng chưa có district, đang tìm từ context/display_name...`);

                  // Sau khi TP.HCM sáp nhập, danh sách phường/xã thay đổi khá nhiều (168 đơn vị cấp xã mới)
                  // → Không dùng danh sách mapping cứng nữa mà ưu tiên parse linh hoạt từ display_name
                  //    để luôn tương thích với danh sách phường mới theo Nghị quyết 1685/NQ-UBTVQH15
                  //    và danh sách 168 phường/xã được công bố chính thức
                  //    (xem thêm: Thư Viện Pháp Luật & Cổng thông tin TP.HCM).

                  // Thử tìm trong display_name xem có tên quận/huyện/thành phố con nào không (không bắt buộc có prefix "Quận")
                  // Logic: Tìm phần nằm sau ward và trước city
                  // NHƯNG bỏ qua "Thủ Đức" vì đây là thành phố trực thuộc, không phải quận
                  const parts = displayName.split(',').map(s => s.trim());
                  let foundWardIndex = -1;
                  
                  // Tìm vị trí của ward trong display_name
                  for (let i = 0; i < parts.length; i++) {
                    const lowerPart = parts[i].toLowerCase();
                    if (lowerPart.includes('phường') || lowerPart.includes('xã')) {
                      foundWardIndex = i;
                      break;
                    }
                  }
                  
                  // Tìm district: phần nằm sau ward và trước city
                  // Thường là phần ngay sau ward (index + 1)
                  if (foundWardIndex >= 0 && foundWardIndex < parts.length - 1) {
                    // Bỏ qua các phần có vẻ là tên địa điểm, đường, khu phố, thành phố
                    const skipPatterns = [
                      /đại học|university|trường|school|hospital|bệnh viện|chợ|market/i,
                      /đường|street|road|nguyễn|trần|lê|phạm|võ/i,
                      /khu phố|tổ|ấp|thôn|xóm/i,
                      /thủ đức/i, // Bỏ qua "Thủ Đức" vì đây là thành phố trực thuộc, không phải quận
                      /^\d+$/, // Chỉ là số (postcode)
                    ];
                    
                    // Tìm từ sau ward trở đi, dừng khi gặp city
                    for (let i = foundWardIndex + 1; i < parts.length; i++) {
                      const part = parts[i];
                      const lowerPart = part.toLowerCase();
                      
                      // Dừng nếu gặp city / quốc gia / postcode
                      if (lowerPart.includes('ho chi minh') || 
                          lowerPart.includes('thành phố') ||
                          lowerPart.includes('việt nam') ||
                          lowerPart.includes('vietnam') ||
                          /^\d{5,6}$/.test(part)) { // Postcode
                        break;
                      }
                      
                      // Bỏ qua nếu là tên địa điểm, đường, khu phố, thành phố
                      const isSkip = skipPatterns.some(pattern => pattern.test(part));
                      if (isSkip) {
                        console.log(`⏭️ [DEBUG] Bỏ qua "${part}" vì không phải district`);
                        continue;
                      }
                      
                      // Nếu không chứa "phường", "xã" và có vẻ là tên quận/huyện/thành phố con
                      if (!lowerPart.includes('phường') && 
                          !lowerPart.includes('xã') && 
                          part.length > 2) {
                        finalDistrict = part;
                        console.log(`✅ [DEBUG] Tìm thấy district từ context (sau ward): "${finalDistrict}"`);
                        break;
                      }
                    }
                  }
                }
              }
            } else {
              console.log(`📍 Địa chỉ chỉ là city name (như "TP.HCM"), không extract ward/district`);
            }
            
            console.log(`📍 Thông tin địa chỉ từ geocoding:`, {
              ward: finalWard || '(không có)',
              district: finalDistrict || '(không có)',
              city: extractedCity,
              fullAddress: bestResult.display_name,
              isDetailedInput: isDetailed,
              addressObjectKeys: Object.keys(addressInfo)
            });
            
            // Lưu thông tin geocoding vào state
            const geocodingInfo = {
              lat,
              lng,
              ward: finalWard,
              district: finalDistrict,
              city: extractedCity,
              displayName: bestResult.display_name
            };
            setGeocodingResult(geocodingInfo);
            setGeocodingStatus("found");
            return geocodingInfo;
    }
        }
      } catch (err) {
        console.warn(`⚠️ [${i + 1}] Lỗi geocoding:`, err.message);
        continue;
      }
    }

    // Không tìm thấy
    setGeocodingStatus("not_found");
    console.warn(`⚠️ Không tìm thấy tọa độ chính xác cho "${trimmedAddress}" sau ${uniqueVariations.length} lần thử`);
    
    if (skipFallback) {
      return null; // Không dùng fallback, để người dùng nhập thủ công
    }
    
    // Fallback: Nếu không tìm thấy, dùng tọa độ trung tâm HCM
    console.warn(`📍 Sử dụng tọa độ mặc định trung tâm TP.HCM: (10.8231, 106.6297)`);
    console.warn(`💡 Lưu ý: Tọa độ này có thể không chính xác, nhưng vẫn có thể tiếp tục đặt hàng`);
    return { lat: 10.8231, lng: 106.6297 }; // Tọa độ trung tâm TP.HCM
  };

  // === Kiểm tra + xử lý thanh toán ===
  const handleCheckout = async () => {
    if (!currentUser) {
      alert("⚠️ Bạn cần đăng nhập để thanh toán!");
      navigate("/login", { state: { from: "/checkout" } });
      return;
    }
    if (cart.length === 0) {
      alert("🛒 Giỏ hàng của bạn đang trống!");
      navigate("/cart");
      return;
    }
    if (!merchantId) {
      alert("⚠️ Không xác định được nhà hàng. Vui lòng thử lại!");
      return;
    }
    if (!form.address || form.address.trim().length < 5) {
      alert("📍 Vui lòng nhập địa chỉ giao hàng cụ thể hơn.");
      return;
    }

    setIsProcessing(true);
    try {
      let coordsResult = customerCoords;
      let geocodingInfo = null;

      if (selectedSavedAddress) {
        if (selectedSavedAddress.lat && selectedSavedAddress.lng) {
          coordsResult = {
            lat: parseFloat(selectedSavedAddress.lat),
            lng: parseFloat(selectedSavedAddress.lng)
          };
          setCustomerCoords(coordsResult);
          setGeocodingResult({
            lat: coordsResult.lat,
            lng: coordsResult.lng,
            ward: selectedSavedAddress.communeName,
            district: selectedSavedAddress.districtName,
            city: selectedSavedAddress.provinceName,
            displayName: selectedSavedAddress.fullAddress
          });
          setGeocodingStatus("found");
        } else {
          geocodingInfo = await getCoordinatesForAddress(
            selectedSavedAddress.fullAddress || selectedSavedAddress.street
          );
        }
      } else {
        geocodingInfo = await getCoordinatesForAddress(form.address);
      }
      
      if (geocodingInfo) {
        coordsResult = { lat: geocodingInfo.lat, lng: geocodingInfo.lng };
        setCustomerCoords(coordsResult);
        console.log("📍 Geocoding result (to be normalized on backend):", geocodingInfo);
      }

      await submitOrder(coordsResult);
    } catch (error) {
      console.error("Lỗi trong quá trình xử lý:", error);
      alert(error?.message || "Có lỗi xảy ra, vui lòng thử lại!");
    } finally {
      setIsProcessing(false);
    }
  };

  // === Tạo đơn hàng sau khi thanh toán / COD ===
  const submitOrder = async (coords = customerCoords) => {
    if (!coords) {
      throw new Error("❗Thiếu tọa độ khách hàng. Vui lòng thử lại.");
    }

    try {
      const userId = currentUser?.id || currentUser?.uid || "unknown";

      // Construct payload for backend API
      // merchantId đã có từ cart (restaurantId trong cart thực chất là merchantId)
      const finalMerchantId = merchantId || restaurantDetails?.merchantId;
      
      if (!finalMerchantId) {
        throw new Error("⚠️ Không xác định được nhà hàng. Vui lòng thử lại!");
      }
      
      // Parse địa chỉ để lấy ward, district, city
      // Ưu tiên: 1) form.ward/district/city, 2) geocoding result (chỉ nếu có), 3) parse từ address, 4) default
      
      let ward = form.ward?.trim() || '';
      let district = form.district?.trim() || '';
      let city = form.city?.trim() || '';
      
      // Kiểm tra xem địa chỉ input có chi tiết không
      const isCityOnly = (addr) => {
        const cityOnlyPatterns = /^(tp\.?hcm|tp\.?hồ\s*chí\s*minh|ho\s*chi\s*minh|hồ\s*chí\s*minh)$/i;
        return cityOnlyPatterns.test(addr.trim());
      };
      
      const isDetailedAddress = (addr) => {
        const hasHouseNumber = /^\d+/.test(addr.trim());
        const hasWardDistrict = /(phường|xã|quận|huyện)/i.test(addr);
        return hasHouseNumber || hasWardDistrict;
      };
      
      const isCityOnlyInput = isCityOnly(form.address);
      const isDetailed = isDetailedAddress(form.address);
      const shouldExtractWardDistrict = isDetailed || !isCityOnlyInput;
      
      // Nếu không có trong form, ưu tiên dùng từ geocoding result
      // NHƯNG chỉ dùng ward/district từ geocoding nếu địa chỉ input có chi tiết
      if (geocodingResult) {
        // City: luôn dùng từ geocoding nếu có
        if (!city && geocodingResult.city) {
          city = geocodingResult.city;
          console.log(`📍 Sử dụng city từ geocoding: ${city}`);
        }
        
        // Ward/District: Dùng nếu địa chỉ có chi tiết HOẶC là địa danh/địa điểm (không phải chỉ là city)
        if (shouldExtractWardDistrict) {
          if (!ward && geocodingResult.ward) {
            ward = geocodingResult.ward;
            console.log(`📍 Sử dụng ward từ geocoding: ${ward}`);
          }
          if (!district && geocodingResult.district) {
            district = geocodingResult.district;
            console.log(`📍 Sử dụng district từ geocoding: ${district}`);
          }
        } else {
          console.log(`📍 Địa chỉ chỉ là city name, không dùng ward/district từ geocoding`);
        }
      }
      
      // Nếu vẫn không có, parse từ address string
      if (!ward || !district) {
        const addressParts = form.address.split(',').map(s => s.trim());
        
        addressParts.forEach(part => {
          const lowerPart = part.toLowerCase();
          if (!ward && (lowerPart.includes('phường') || lowerPart.includes('xã'))) {
            ward = part.replace(/^(Phường|Xã)\s*/i, '').trim();
          } else if (!district && (lowerPart.includes('quận') || lowerPart.includes('huyện'))) {
            district = part.replace(/^(Quận|Huyện)\s*/i, '').trim();
          } else if (!city && (lowerPart.includes('thành phố') || lowerPart.includes('tp') || lowerPart.includes('hồ chí minh'))) {
            city = part.replace(/^(Thành phố|TP\.?)\s*/i, '').trim() || 'Ho Chi Minh';
          }
        });
      }
      
      // Đảm bảo có giá trị mặc định nếu vẫn trống (backend yêu cầu NotBlank)
      // Nếu địa chỉ không chi tiết (chỉ có city), dùng giá trị generic hợp lý
      if (!ward || ward.length < 2) {
        if (isDetailed) {
          console.warn(`⚠️ Không tìm thấy ward từ địa chỉ chi tiết, dùng giá trị mặc định`);
          ward = 'Not Specified';
        } else {
          console.log(`📍 Địa chỉ chỉ có city, ward không được chỉ định`);
          ward = 'Not Specified'; // Địa chỉ chỉ có city, không có ward cụ thể
        }
      }
      if (!district || district.length < 2) {
        if (isDetailed) {
          console.warn(`⚠️ Không tìm thấy district từ địa chỉ chi tiết, dùng giá trị mặc định`);
          district = 'Not Specified';
        } else {
          console.log(`📍 Địa chỉ chỉ có city, district không được chỉ định`);
          district = 'Not Specified'; // Địa chỉ chỉ có city, không có district cụ thể
        }
      }
      if (!city || city.length < 2) {
        city = 'Ho Chi Minh'; // Default city
      }
      
      console.log(`📦 Final address fields (before backend normalization):`, { ward, district, city, isDetailed });
      
      if (selectedSavedAddress) {
        ward = selectedSavedAddress.communeName || ward;
        district = selectedSavedAddress.districtName || district;
        city = selectedSavedAddress.provinceName || city;
      }

      const orderPayload = {
        userId: Number(userId),
        merchantId: Number(finalMerchantId),
        discount: 0,
        shippingFee: 15000,
        note: `Giao đến ${form.address}`,
        deliveryAddressId: selectedAddressId || null,
        deliveryAddress: {
          receiverName: `${form.lastName} ${form.firstName}`.trim(),
          receiverPhone: form.phone,
          addressLine1: form.address,
          ward: ward,
          district: district,
          city: city,
          lat: coords.lat ? parseFloat(coords.lat) : null,
          lng: coords.lng ? parseFloat(coords.lng) : null
        },
        orderItems: cart.map((item) => ({
          productId: Number(item.id),
          quantity: item.quantity
        }))
      };

      console.log('📦 Order payload:', JSON.stringify(orderPayload, null, 2));

      // Call backend API
      const response = await createOrder(orderPayload);
      console.log('✅ [Checkout] Order created successfully:', response);
      console.log('✅ [Checkout] Order ID:', response?.id);
      console.log('✅ [Checkout] Order Code:', response?.orderCode);

      if (!response || !response.id) {
        console.error('❌ [Checkout] Order response không có ID:', response);
        throw new Error("Đơn hàng đã được tạo nhưng không có ID. Vui lòng kiểm tra lại!");
      }

      try {
        const fallbackTotal =
          Number(total) +
          Number(orderPayload.shippingFee || 0) -
          Number(orderPayload.discount || 0);
        const storedMeta = {
          method: paymentMethod,
          createdAt: new Date().toISOString(),
          currency: response.currency || "VND",
          grandTotal: response.grandTotal
            ? Number(response.grandTotal)
            : fallbackTotal
        };
        localStorage.setItem(`${ORDER_PAYMENT_META_PREFIX}${response.id}`, JSON.stringify(storedMeta));
      } catch (storageError) {
        console.warn("⚠️ Không thể lưu thông tin phương thức thanh toán:", storageError);
      }

      setCart([]);
      const identifier = currentUser?.id || currentUser?.uid || currentUser?.username;
      if (identifier) {
        localStorage.removeItem(`cart_${identifier}`);
      }

      setShowSuccessPopup(true);
      // Use response.id from backend
      console.log(`🚀 [Checkout] Navigating to /waiting/${response.id}`);
      setTimeout(() => navigate(`/waiting/${response.id}`), 1000);
    } catch (err) {
      console.error("❌ Lỗi lưu order:", err);
      const errorMessage =
        err?.response?.data?.message ||
        err?.message ||
        "Có lỗi xảy ra khi đặt hàng, vui lòng thử lại!";
      throw new Error(errorMessage);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isProcessing) return;
    await handleCheckout();
  };

  return (
    <div className="checkout-page">
      <div className="checkout-header">
        <Link to="/cart">
          <button className="checkout-back-btn">⬅ Quay lại giỏ hàng</button>
        </Link>
        <h2>🔒 THÔNG TIN ĐẶT HÀNG</h2>
      </div>

      <div className="checkout-container">
        {/* ===== CỘT TRÁI ===== */}
        <div className="checkout-info">
          <div className="checkout-info-block">
            <h3>ĐƯỢC GIAO TỪ:</h3>
            <p className="store-name">{restaurantDetails ? restaurantDetails.name : "Đang tải..."}</p>
            <p className="store-address">{restaurantDetails ? restaurantDetails.address : "..."}</p>
          </div>

          <div className="checkout-info-block">
            <h3>GIAO ĐẾN:</h3>
            {savedAddresses.length > 0 && (
              <div className="saved-address-select">
                <label style={{ display: "flex", justifyContent: "space-between", fontWeight: "600" }}>
                  <span>Địa chỉ đã lưu</span>
                  <Link to="/profile" style={{ fontSize: "13px", color: "#2563eb" }}>Quản lý</Link>
                </label>
                <select
                  value={selectedAddressId || ""}
                  onChange={(e) => setSelectedAddressId(e.target.value ? Number(e.target.value) : null)}
                >
                  {savedAddresses.map(addr => (
                    <option key={addr.id} value={addr.id}>
                      {addr.fullAddress || addr.street}
                    </option>
                  ))}
                  <option value="">-- Nhập địa chỉ mới --</option>
                </select>
              </div>
            )}
            <input
              type="text"
              name="address"
              value={form.address}
              onChange={handleChange}
              placeholder="Nhập địa chỉ giao hàng..."
              className="address-input"
            />
            
            {/* Trạng thái geocoding */}
            {geocodingStatus === "searching" && (
              <p style={{ color: "#1890ff", fontSize: "14px", margin: "10px 0" }}>
                🔍 Đang tìm tọa độ...
              </p>
            )}
            {geocodingStatus === "found" && customerCoords && (
              <p style={{ color: "#52c41a", fontSize: "14px", margin: "10px 0" }}>
                ✅ Đã tìm thấy tọa độ: ({customerCoords.lat.toFixed(6)}, {customerCoords.lng.toFixed(6)})
              </p>
            )}
            {geocodingStatus === "not_found" && (
              <div style={{ margin: "10px 0", padding: "10px", background: "#fff7e6", borderRadius: "5px", border: "1px solid #ffd591" }}>
                <p style={{ color: "#fa8c16", fontSize: "14px", marginBottom: "10px" }}>
                  ⚠️ Không tìm thấy tọa độ chính xác. Bạn có thể:
                </p>
                <label style={{ display: "flex", alignItems: "center", marginBottom: "10px", cursor: "pointer" }}>
                  <input
                    type="checkbox"
                    checked={useManualCoords}
                    onChange={(e) => {
                      setUseManualCoords(e.target.checked);
                      if (!e.target.checked) {
                        setManualCoords({ lat: "", lng: "" });
                      }
                    }}
                    style={{ marginRight: "8px" }}
                  />
                  <span>Nhập tọa độ thủ công</span>
                </label>
                {useManualCoords && (
                  <div style={{ display: "flex", gap: "10px", marginTop: "10px" }}>
                    <input
                      type="number"
                      step="any"
                      placeholder="Vĩ độ (Lat)"
                      value={manualCoords.lat}
                      onChange={(e) => setManualCoords({ ...manualCoords, lat: e.target.value })}
                      style={{ flex: 1, padding: "8px", border: "1px solid #d9d9d9", borderRadius: "4px" }}
                    />
                    <input
                      type="number"
                      step="any"
                      placeholder="Kinh độ (Lng)"
                      value={manualCoords.lng}
                      onChange={(e) => setManualCoords({ ...manualCoords, lng: e.target.value })}
                      style={{ flex: 1, padding: "8px", border: "1px solid #d9d9d9", borderRadius: "4px" }}
                    />
                  </div>
                )}
                <p style={{ color: "#8c8c8c", fontSize: "12px", marginTop: "10px" }}>
                  💡 Hoặc để trống để dùng tọa độ mặc định (trung tâm TP.HCM)
                </p>
              </div>
            )}
            
            <iframe
              title="map"
              src={`https://maps.google.com/maps?q=${encodeURIComponent(form.address)}&t=&z=15&ie=UTF8&iwloc=&output=embed`}
              width="100%"
              height="300"
              style={{ border: 0, margin: "20px 0", borderRadius: "10px" }}
            />
          </div>
        </div>

        {/* ===== CỘT PHẢI ===== */}
        <aside className="checkout-summary">
          <div className="summary-card">
            <h3>TÓM TẮT ĐƠN HÀNG:</h3>
            <ul>
              {cart.map((item) => (
                <li key={item.id} className="summary-item">
                  <span>{item.quantity} x {item.name}</span>
                  <span>{(item.price * item.quantity).toLocaleString()}₫</span>
                </li>
              ))}
            </ul>
            <div className="summary-line total">
              <span>Tổng thanh toán</span>
              <strong>{total.toLocaleString()}₫</strong>
            </div>
          </div>

          {/* 🧾 THÔNG TIN KHÁCH HÀNG */}
          <div className="customer-info-card">
            <h2>THÔNG TIN KHÁCH HÀNG:</h2>
            <form onSubmit={handleSubmit} className="checkout-form">
              <div className="form-group-inline">
                <div className="form-group">
                  <label>Họ</label>
                  <input type="text" name="lastName" value={form.lastName} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Tên</label>
                  <input type="text" name="firstName" value={form.firstName} onChange={handleChange} required />
                </div>
              </div>

              <div className="form-group">
                <label>Số điện thoại</label>
                <input type="tel" name="phone" value={form.phone} onChange={handleChange} required />
              </div>

              {/* 💳 CHỌN PHƯƠNG THỨC THANH TOÁN */}
              <div className="payment-section">
                <h2>Phương thức thanh toán</h2>
                <div className="payment-option">
                  <input
                    type="radio"
                    id="qr"
                    name="paymentMethod"
                    value="qr"
                    checked={paymentMethod === "qr"}
                    onChange={(e) => setPaymentMethod(e.target.value)}
                  />
                  <label htmlFor="qr">
                    Thanh toán bằng quét mã QR
                  </label>
                </div>
              </div>

              <button type="submit" className="checkout-btn-primary" disabled={isProcessing}>
                {isProcessing ? "Đang xử lý..." : "Xác nhận đặt hàng"}
              </button>
            </form>
          </div>
        </aside>
      </div>

      {/* 🎉 POPUP SUCCESS */}
      {showSuccessPopup && (
        <div className="success-popup">
          <div className="success-popup-content">
            <h2>🎉 Đặt hàng thành công!</h2>
          </div>
        </div>
      )}
    </div>
  );
}
