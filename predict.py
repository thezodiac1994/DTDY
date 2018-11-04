from flask import Flask
from flask_restful import Api, Resource, reqparse
from flask import Flask, request

app = Flask(__name__)
api = Api(app)


from clarifai.rest import ClarifaiApp
api_key = ClarifaiApp(api_key='97246cf0da164fd9963b504cccaca5ba')

from clarifai.rest import Image as CImage
import json



# this function returns the ingredients queried on clarifai
def get_ingreds(img_path,url_flag=1):
    model = api_key.models.get('food-items-v1.0')
    
    image = None
    if(not url_flag):
        image = CImage(file_obj=open(img_path,'rb'))
    else:
        image = CImage(url=imgpath)
        
    response = model.predict([image])
    parsed_response = json.loads(json.dumps(response))

    json_dict = parsed_response['outputs'][0]['data']['concepts']

    for i in json_dict:
        print(i)
        
    return json.dumps(json_dict)


# this function returns matching recipe names for the given response 

def get_matching_recipes(json_resp, recipes, url_flag = 1):
    
    # define matchingrecipes as finding the word in the list of recipes with some modifications*
    
    parsed_response = json.loads(json_resp)
    recipe_score = {}
    
    # apply tfidf --- read from offline file 
    tfidf = {}
    with open('all_stop_words.txt', 'r') as F:
        lines = F.readlines()
        for line in lines:
            words = line.replace('\n', '').split(' ')
            tfidf[words[0]] = int(words[1])
    
   # print(tfidf)
    
    for response in parsed_response:
        search_words = response['name'].lower().replace('-',' ').split(' ')
        #print('word in image :', search_words)
        for word in search_words:
            for ids,recipe_name in recipes.items():
                keywords = recipe_name.lower().replace('-',' ').split(' ')
                #print('recipe name is ', recipe_name)
                #print('keywords in recipe: ', keywords)
                if word in keywords:
                    #print('found word = ' , word, 'include ', recipe_name)
                    
                    word_score = 0
                    if(word in tfidf):
                        word_score = 1/tfidf[word]
                    else:
                        word_score = 1
                    
                    if(ids in recipe_score):
                        recipe_score[ids] += word_score
                    else:
                        recipe_score[ids] = word_score
                
                
        
    recipe_score = sorted(recipe_score.items(), key=lambda x: -x[1])
    return recipe_score
    
def predict_ids(imgpath, url_flag=1):
    with open('recipes.json', 'r') as F:
        json_recipes = json.load(F)

    recipes = {}
    for data in json_recipes['recipes']:
        recipes[data['id']] = data['name']

    #print(recipes)
    matching_ids = get_matching_recipes(get_ingreds(imgpath,url_flag), recipes, url_flag)
    return matching_ids[:5]  # return top 5
    
    


users = [
    {
        "name": "Nicholas",
        "age": 42,
        "occupation": "Network Engineer"
    },
    {
        "name": "Elvin",
        "age": 32,
        "occupation": "Doctor"
    },
    {
        "name": "Jass",
        "age": 22,
        "occupation": "Web Developer"
    }
]

class User(Resource):
    def get(self, name):
        print('name is : ', name)
        #print(request.headers)
        url = 'https://firebasestorage.googleapis.com/v0/b/ubhacking-221502.appspot.com/o/images%2FfoodImage.jpg?alt=media&token=941439d4-ef87-4d95-9fc9-72ecb118ec86'
        print('url is : ', url)
        ids = predict_ids(url, 1)
        return [i[0] for i in ids]

        '''
        from PIL import Image
        import requests
        from io import BytesIO

        response = requests.get(url)
        img = Image.open(BytesIO(response.content))'''
        

    def post(self, name):
        parser = reqparse.RequestParser()
        parser.add_argument("age")
        parser.add_argument("occupation")
        args = parser.parse_args()

        for user in users:
            if(name == user["name"]):
                return "User with name {} already exists".format(name), 400

        user = {
            "name": name,
            "age": args["age"],
            "occupation": args["occupation"]
        }
        users.append(user)
        return user, 201

    def put(self, name):
        parser = reqparse.RequestParser()
        parser.add_argument("age")
        parser.add_argument("occupation")
        args = parser.parse_args()

        for user in users:
            if(name == user["name"]):
                user["age"] = args["age"]
                user["occupation"] = args["occupation"]
                return user, 200
        
        user = {
            "name": name,
            "age": args["age"],
            "occupation": args["occupation"]
        }
        users.append(user)
        return user, 201

    def delete(self, name):
        global users
        users = [user for user in users if user["name"] != name]
        return "{} is deleted.".format(name), 200
      
api.add_resource(User, "/user/<string:name>")

app.run(debug=True)