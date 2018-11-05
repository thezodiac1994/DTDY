import json
import http.client, urllib.request, urllib.parse, urllib.error, base64
import gzip

class DTDYWegmans(object):

    def __init__(self):
        self._headers = {'Subscription-Key': '{Add your key}'}
        self._params = urllib.parse.urlencode
        self.conn = http.client.HTTPSConnection('api.wegmans.io')


    def get_all_recipes(self):
        self.conn.request("GET", "/meals/recipes?api-version=2018-10-18&%s" % self._params({}), "{body}", self._headers)
        response = self.conn.getresponse()
        data = gzip.decompress(response.read())
        print(data.decode('utf-8'))
        # with open('recipes.json', 'w') as f:
        #     f.write(data.decode('utf-8'))
        self.conn.close()

    def get_recipe_by_ids(self, list_ids):
        """
            Returns list of ids to fetch the recipe ingredients
        """
        data = []
        for id in list_ids:
            self.conn.request('GET', "/meals/recipes/{id}?api-version=2018-10-18&%s" % self._params({'id':id}), "{body}", self._headers)
            response = self.conn.getresponse()
            data.append(response.read())
            self.conn.close()
        return data

    def _parse_ingred(self, ingred):
        fields = ['displayOrder','name']
        data = []
        for ing in ingred:
            temp = {f: ing[f] for f in fields}
            temp['quantity'] = ing['quantity']['text']
            if 'sku' not in ing:
                temp['sku'] = "None" #[ing['_links'] if '_links' in ing else "None"]
                temp['price'] = '0.00'
            else:
                temp['sku'] = ing['sku']
                # print(int(ing['sku']))
                temp['price'] = self._get_price(ing['sku'])
            # print(temp['name'], temp['price'])            
            data.append(temp)
        total_price = 0.0
        for d in data:
            total_price += float(d['price'])

        return data, total_price

    def get_ingred_by_ids(self, list_ids):
        data = []
        for id in list_ids:
            self.conn.request('GET', "/meals/recipes/{id}?api-version=2018-10-18&%s" % self._params({'id':id}), "{body}", self._headers)
            response = self.conn.getresponse()
            jresp = json.loads(response.read().decode('utf-8'))
            ingred, total_price = self._parse_ingred(jresp['ingredients'])
            # print(jresp)
            data.append({"id":jresp['id'],
                                    "name": jresp['name'],
                                    "ingredients": ingred,
                                    "total_price": total_price,
                                    "nutrition":jresp['nutrition'],
                                    "preptime":jresp['preparationTime'],
                                    "recipe":jresp['instructions']['directions']})
            self.conn.close()
        return data

    def _get_price(self, sku):
        self.conn.request("GET", "/products/{}/prices?api-version=2018-10-18".format(sku), "{body}", self._headers)
        response = self.conn.getresponse()
        jresp = json.loads(response.read().decode('utf-8'))
        return jresp['stores'][0]['price']

    def __len__(self):
        return len(self)


    def __show__(self):
        pass



    def _load_json(self, filename):
        """
            loads json file
        """
        with open(filename, 'r') as f:
            data = f.read()
        return json.loads(data)


if __name__ == '__main__':
    dw = DTDYWegmans()
    ids = ['19315','6768','21388']
    # dw.get_all_recipes()
    # ingred = dw.get_recipe_by_ids(ids)[0]
    # # print(ingred)

    # for x in ingred:
    #     print(x)
    # fetches ingredients, nutrition and price
    ret = dw.get_ingred_by_ids(ids)
    with open('ingred.json','w') as f:
        f.write(json.dumps(ret))
    # pass