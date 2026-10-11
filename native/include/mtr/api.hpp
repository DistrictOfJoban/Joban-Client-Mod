#pragma once
#include "mtr_native.h"
#include "detail/json.hpp"
#include <functional>
#include <initializer_list>
#include <memory>
#include <stdexcept>
#include <string>

namespace mtr {
using Json = nlohmann::json;

/** Uses exactly the public Java objects documented for JavaScript scripts. */
class Api {
public:
    struct Channel {
        void* user;
        decltype(JcmHostServices::invoke_api) invoke;
        Json request(const Json& command) const {
            std::string bytes = command.dump();
            int32_t size = 0;
            const char* data = invoke(user, bytes.data(), static_cast<int32_t>(bytes.size()), &size);
            if (!data || size < 0) throw std::runtime_error("Native API unavailable");
            Json reply = Json::parse(data, data + size);
            if (reply.contains("error")) throw std::runtime_error("Native API " + command.value("member", command.value("name", command.value("op", "request"))) + ": " + reply["error"].get<std::string>());
            return reply.at("value");
        }
    };

    class Object {
        struct Reference {
            std::shared_ptr<Channel> channel;
            Json value;
            ~Reference() {
                if (value.is_object() && value.contains("$handle")) {
                    try { channel->request({{"op", "release"}, {"handle", value["$handle"]}}); }
                    catch (...) {} // session may already have been disposed
                }
            }
        };
        std::shared_ptr<Reference> reference_;
        Object(std::shared_ptr<Channel> channel, Json value)
            : reference_(new Reference{std::move(channel), std::move(value)}) {}
        friend class Api;
    public:
        Object() = default;
        bool is_null() const { return !reference_ || reference_->value.is_null(); }
        const Json& json() const { static const Json null; return reference_ ? reference_->value : null; }
        template <typename T> T as() const { return json().get<T>(); }
        std::string enum_name() const { return json().at("name").get<std::string>(); }
        Object call(const std::string& member, Json args) const {
            if (is_null()) throw std::runtime_error("Call on null native object");
            return Object(reference_->channel, reference_->channel->request({{"op","call"},{"target",json()},{"member",member},{"args",args}}));
        }
        Object call(const std::string& member, std::initializer_list<Json> args = {}) const {
            return call(member, Json(Json::array_t(args)));
        }
        Object get(const std::string& member) const {
            if (is_null()) throw std::runtime_error("Get on null native object");
            return Object(reference_->channel, reference_->channel->request({{"op","get"},{"target",json()},{"member",member}}));
        }
        void set(const std::string& member, Json value) const {
            if (is_null()) throw std::runtime_error("Set on null native object");
            reference_->channel->request({{"op","set"},{"target",json()},{"member",member},{"value",std::move(value)}});
        }
        void close() const { if (!is_null()) call("close"); }
        friend void to_json(Json& json, const Object& object) { json = object.json(); }
    };

    class Callback {
        struct Holder { std::function<Json(Json)> function; };
        std::unique_ptr<Holder> holder_;
        static const char* run(void* user, const char* parameters, int32_t length, int32_t* size) noexcept {
            auto& holder = *static_cast<Holder*>(user);
            static thread_local std::string result;
            try { result = Json({{"value",holder.function(Json::parse(parameters, parameters + length))}}).dump(); }
            catch (const std::exception& e) { result = Json({{"error",e.what()}}).dump(); }
            catch (...) { result = "{\"error\":\"Unknown native callback exception\"}"; }
            *size = static_cast<int32_t>(result.size());
            return result.data();
        }
    public:
        explicit Callback(std::function<Json(Json)> function) : holder_(new Holder{std::move(function)}) {}
        Callback(Callback&&) = default;
        Callback& operator=(Callback&&) = default;
        Json json() const {
            return {{"$callback", {{"function",std::to_string(reinterpret_cast<uintptr_t>(&run))},
                                  {"user",std::to_string(reinterpret_cast<uintptr_t>(holder_.get()))}}}};
        }
        friend void to_json(Json& json, const Callback& callback) { json = callback.json(); }
    };

    explicit Api(const JcmFrameInput& input) {
        if (input.abi_version < 8 || !input.host || !input.host->invoke_api) throw std::runtime_error("ABI 8 host required");
        channel_ = std::make_shared<Channel>(Channel{input.host->user, input.host->invoke_api});
    }
    Object root(const std::string& name) const { return wrap(channel_->request({{"op","root"},{"name",name}})); }
    Object context() const { return root("context"); }
    Object wrapper() const { return root("wrapper"); }
    Json input() const { return channel_->request({{"op","root"},{"name","SCRIPT_INPUT"}}); }
    Object create(const std::string& type, Json args) const {
        return wrap(channel_->request({{"op","new"},{"class",type},{"args",args}}));
    }
    Object create(const std::string& type, std::initializer_list<Json> args = {}) const {
        return create(type, Json(Json::array_t(args)));
    }
    Object call(const std::string& type, const std::string& member, Json args) const {
        return wrap(channel_->request({{"op","call"},{"class",type},{"member",member},{"args",args}}));
    }
    Object call(const std::string& type, const std::string& member, std::initializer_list<Json> args = {}) const {
        return call(type, member, Json(Json::array_t(args)));
    }
    Object get(const std::string& type, const std::string& member) const {
        return wrap(channel_->request({{"op","get"},{"class",type},{"member",member}}));
    }
    Object wrap(Json value) const { return Object(channel_, std::move(value)); }
private:
    std::shared_ptr<Channel> channel_;
};
} // namespace mtr
